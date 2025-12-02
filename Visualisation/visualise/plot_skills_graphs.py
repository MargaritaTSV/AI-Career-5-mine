"""
Generate skill graph visualisations.

The script replicates the pruning/layout logic used in earlier experiments:
- Builds a directed graph from the skills-graph.json resource.
- Keeps a spanning forest with at most one parent per node, prioritising heavier edges.
- Adds a handful of extra strong edges for readability.
- Computes a layered layout and draws two images:
  1) Base skills graph.
  2) Graph coloured by mastery (green) vs. required-to-learn (light blue).

Usage:
    python visualise/plot_skills_graphs.py

Outputs are written to target/visualisations/skills_graph.png and
skills_graph_mastery.png.
"""
from __future__ import annotations

import json
from collections import defaultdict, deque
from pathlib import Path
from typing import Dict, Iterable, List, Tuple

import matplotlib.pyplot as plt
import networkx as nx

PROJECT_ROOT = Path(__file__).resolve().parents[2]
VISUALISATION_ROOT = Path(__file__).resolve().parents[1]

GRAPH_PATH = VISUALISATION_ROOT / "graphs" / "skills-graph.json"
USER_MATRIX_PATH = PROJECT_ROOT / "src" / "main" / "resources" / "matrices" / "user_skill_matrix.json"
DESIRED_MATRIX_PATH = PROJECT_ROOT / "src" / "main" / "resources" / "matrices" / "desired_role_matrix.json"
OUTPUT_DIR = VISUALISATION_ROOT / "target" / "visualisations"

LIGHT_ORANGE = "#ffd8a6"
ORANGE_BORDER = "#f4a261"
TEXT_COLOR = "#3a3a3a"
MASTERED_COLOR = "#b8f5b1"  # light green
MISSING_COLOR = "#c9e3ff"   # light blue


def load_graph() -> Tuple[nx.DiGraph, List[Tuple[str, str, int]]]:
    with GRAPH_PATH.open("r", encoding="utf-8") as f:
        data = json.load(f)

    nodes = data["nodes"]
    edges = data["edges"]

    graph = nx.DiGraph()
    for node in nodes:
        graph.add_node(node["id"], vacancies=node.get("vacancies", 0))

    edge_list: List[Tuple[str, str, int]] = []
    for edge in edges:
        weight = int(edge.get("weight", 0))
        edge_list.append((edge["from"], edge["to"], weight))
        graph.add_edge(edge["from"], edge["to"], weight=weight)

    return graph, sorted(edge_list, key=lambda x: x[2], reverse=True)


def build_pruned_graph(graph: nx.DiGraph, sorted_edges: List[Tuple[str, str, int]]) -> nx.DiGraph:
    tree = nx.DiGraph()
    for node, attrs in graph.nodes(data=True):
        tree.add_node(node, **attrs)

    in_parent = set()

    for u, v, weight in sorted_edges:
        if v in in_parent:
            continue
        if nx.has_path(tree, v, u):
            continue
        tree.add_edge(u, v, weight=weight)
        in_parent.add(v)

    extra_edges_to_add = 4
    added_extra = 0
    for u, v, weight in sorted_edges:
        if added_extra >= extra_edges_to_add:
            break
        if tree.has_edge(u, v):
            continue
        if nx.has_path(tree, v, u):
            continue
        tree.add_edge(u, v, weight=weight)
        added_extra += 1

    return tree


def compute_levels(graph: nx.DiGraph) -> Dict[str, int]:
    roots = [n for n in graph.nodes() if graph.in_degree(n) == 0]
    level: Dict[str, int] = {n: None for n in graph.nodes()}
    queue: deque[str] = deque()

    for root in roots:
        level[root] = 0
        queue.append(root)

    while queue:
        current = queue.popleft()
        for succ in graph.successors(current):
            if level[succ] is None or level[succ] > level[current] + 1:
                level[succ] = level[current] + 1
                queue.append(succ)

    for node in graph.nodes():
        if level[node] is None:
            level[node] = 0
            if node not in roots:
                roots.append(node)

    return level


def compute_positions(level: Dict[str, int]) -> Dict[str, Tuple[float, float]]:
    levels_to_nodes: Dict[int, List[str]] = defaultdict(list)
    for node, lvl in level.items():
        levels_to_nodes[lvl].append(node)

    positions: Dict[str, Tuple[float, float]] = {}
    for lvl, nodes_on_level in levels_to_nodes.items():
        count = len(nodes_on_level)
        for i, node in enumerate(sorted(nodes_on_level)):
            x = 0.0 if count == 1 else -1.0 + 2.0 * i / (count - 1)
            y = -lvl
            positions[node] = (x, y)

    if "sql" in positions and "machine_learning" in positions:
        positions["sql"], positions["machine_learning"] = (
                positions["machine_learning"],
                positions["sql"],
        )

    return positions


def edge_widths(graph: nx.DiGraph) -> List[float]:
    weights = nx.get_edge_attributes(graph, "weight")
    if not weights:
        return [2.0 for _ in graph.edges()]

    values = list(weights.values())
    w_min, w_max = min(values), max(values)
    if w_max == w_min:
        return [2.0 for _ in graph.edges()]

    widths: List[float] = []
    for edge in graph.edges():
        weight = weights.get(edge, 0)
        norm = (weight - w_min) / (w_max - w_min)
        widths.append(1.2 + 2.0 * norm)
    return widths


def load_matrix(path: Path) -> Dict[str, int]:
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


def classify_nodes(graph: nx.DiGraph) -> Dict[str, str]:
    user_matrix = load_matrix(USER_MATRIX_PATH)
    desired_matrix = load_matrix(DESIRED_MATRIX_PATH)

    mastered = {skill for skill, flag in user_matrix.items() if flag == 1}
    required = {skill for skill, flag in desired_matrix.items() if flag == 1}
    missing = required - mastered

    status: Dict[str, str] = {}
    for node in graph.nodes():
        if node in mastered:
            status[node] = "mastered"
        elif node in missing:
            status[node] = "missing"
        else:
            status[node] = "neutral"
    return status


def draw_graph(graph: nx.DiGraph, positions: Dict[str, Tuple[float, float]], *,
               node_colors: Iterable[str], output: Path) -> None:
    vacancies = nx.get_node_attributes(graph, "vacancies")
    node_sizes = [80 + 3 * vacancies.get(node, 0) for node in graph.nodes()]

    widths = edge_widths(graph)

    plt.figure(figsize=(10, 8))

    nx.draw_networkx_nodes(
        graph,
        positions,
        node_size=node_sizes,
        alpha=0.95,
        linewidths=1.5,
        edgecolors=ORANGE_BORDER,
        node_color=node_colors,
    )

    nx.draw_networkx_edges(
        graph,
        positions,
        width=widths,
        arrows=True,
        arrowstyle='-|>',
        arrowsize=20,
        connectionstyle="arc3,rad=0.12",
        edge_color=ORANGE_BORDER,
    )

    nx.draw_networkx_labels(
        graph,
        positions,
        font_size=11,
        font_weight="bold",
        font_color=TEXT_COLOR,
    )

    plt.axis("off")
    plt.margins(0.25)
    plt.tight_layout()

    output.parent.mkdir(parents=True, exist_ok=True)
    plt.savefig(output, dpi=300, bbox_inches="tight")
    plt.close()



def main() -> None:
    full_graph, sorted_edges = load_graph()
    pruned_graph = build_pruned_graph(full_graph, sorted_edges)
    levels = compute_levels(pruned_graph)
    positions = compute_positions(levels)

    draw_graph(
        pruned_graph,
        positions,
        node_colors=[LIGHT_ORANGE for _ in pruned_graph.nodes()],
        output=OUTPUT_DIR / "skills_graph.png",
    )

    statuses = classify_nodes(pruned_graph)
    color_map = [
        MASTERED_COLOR if statuses[node] == "mastered"
        else MISSING_COLOR if statuses[node] == "missing"
        else LIGHT_ORANGE
        for node in pruned_graph.nodes()
    ]

    draw_graph(
        pruned_graph,
        positions,
        node_colors=color_map,
        output=OUTPUT_DIR / "skills_graph_mastery.png",
    )


if __name__ == "__main__":
    main()
