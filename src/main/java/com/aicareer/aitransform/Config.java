package com.aicareer.aitransform;

import java.util.Scanner;

public final class Config {

  private static String apiKey;

  private Config() {
  }

  public static synchronized void setApiKey(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("API key must not be empty");
    }
    apiKey = value.trim();
  }

  public static synchronized boolean isApiKeySet() {
    return apiKey != null && !apiKey.isBlank();
  }

  public static synchronized String getApiKey() {
    if (!isApiKeySet()) {
      throw new IllegalStateException("API key is not configured");
    }
    return apiKey;
  }

  public static void ensureApiKeyFromInput(Scanner in) {
    if (isApiKeySet()) {
      return;
    }

    System.out.print("Введите OpenAI API ключ: ");
    String value = in.nextLine().trim();
    while (value.isBlank()) {
      System.out.print("Ключ не может быть пустым, попробуйте ещё раз: ");
      value = in.nextLine().trim();
    }

    setApiKey(value);
  }
}
