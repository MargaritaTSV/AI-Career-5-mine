package com.aicareer.hh.tools;

import com.aicareer.hh.hhapi.HhApiClient;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class AuthProbe {
    // ВСТАВЬ СВОИ
    static final String USER_AGENT = "AI-Career/0.1 (+https://github.com/<yourrepo>; email: youmail@gmail.com)";
    static final String TOKEN = System.getenv("HH_TOKEN"); // или положи строкой

    public static void main(String[] args) throws Exception {
        var client = new HhApiClient(USER_AGENT, TOKEN);

        // 1) Проверяем: токен живой?
        try {
            HttpResponse<String> me = client.get("/me", Map.of());
            System.out.println("[OK] /me доступен, токен валиден, код=" + me.statusCode());
        } catch (RuntimeException e) {
            System.out.println("[AUTH] /me недоступен: " + e.getMessage());
            System.out.println("Вывод: токен отсутствует/протух/не имеет нужных scope.");
        }

        // 2) Проверяем лимиты на публичном эндпоинте с токеном
        Map<String,String> qs = new HashMap<>();
        qs.put("text", "java developer");
        qs.put("per_page", "1");

        int ok = 0, tooMany = 0;
        long first429Wait = -1;

        for (int i = 1; i <= 120; i++) {
            try {
                var r = client.get("/vacancies", qs);
                ok++;
                System.out.println(i + ") 200 OK, len=" + r.body().length());
                Thread.sleep(200); // чуть-чуть спим, чтобы не убиться слишком быстро
            } catch (RuntimeException ex) {
                String msg = ex.getMessage();
                if (msg.contains(" 429 ")) {
                    tooMany++;
                    System.out.println(i + ") 429 Too Many Requests → поймали лимит. " +
                            "Смотри Retry-After/X-RateLimit заголовки в логах выше.");
                    if (first429Wait < 0) first429Wait = System.currentTimeMillis();
                    Thread.sleep(3000);
                } else if (msg.contains(" 401 ")) {
                    System.out.println(i + ") 401 Unauthorized → токен недействителен/протух.");
                    break;
                } else if (msg.contains(" 403 ")) {
                    System.out.println(i + ") 403 Forbidden → вероятно insufficient_scope/ограничение на эндпоинт.");
                    break;
                } else {
                    System.out.println(i + ") FAIL: " + msg);
                    break;
                }
            }
        }

        System.out.println("ИТОГ: OK=" + ok + ", 429=" + tooMany);
        System.out.println("Выше в консоли уже напечатаны заголовки с лимитами (см. X-RateLimit-*, Retry-After).");
    }
}
