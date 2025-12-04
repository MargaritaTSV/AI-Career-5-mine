package com.aicareer.aitransform;

public final class Config {

  private static volatile String apiKey;

  private Config() {
  }

  public static void setApiKey(String key) {
    apiKey = key;
  }

  public static String getApiKey() {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("API key is not configured. Please set it at startup.");
    }
    return apiKey;
  }
}
