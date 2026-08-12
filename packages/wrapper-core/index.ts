import { TransistorCoreInterface } from "@transistorjs/wrapper-types";

/**
 * Client core SDK for Transistor Web Apps.
 */
export class WrapperCore {
  private fallbackToastCallback?: (message: string) => void;

  constructor(options?: {
    onFallbackToast?: (message: string) => void;
  }) {
    this.fallbackToastCallback = options?.onFallbackToast;
  }

  /**
   * Checks if the app is currently running inside the native Android wrapper app.
   */
  isNativeWrapper(): boolean {
    return typeof window !== "undefined" && typeof window.TransistorCore !== "undefined";
  }

  /**
   * Triggers a fallback action, logging a warning and optionally triggering a custom toast callback.
   */
  private triggerFallback(message: string): void {
    console.warn(`[TransistorWrapper SDK] ${message}`);
    if (this.fallbackToastCallback) {
      this.fallbackToastCallback(message);
    }
  }

  /**
   * Gets the native app version.
   * Fallback: returns "0.0.0-web-fallback"
   */
  getAppVersion(): string {
    if (this.isNativeWrapper() && window.TransistorCore) {
      try {
        return window.TransistorCore.getAppVersion();
      } catch (err) {
        console.error("Failed to fetch native app version:", err);
      }
    }
    this.triggerFallback("Running outside native wrapper. Falling back to web-fallback version.");
    return "0.0.0-web-fallback";
  }

  /**
   * Gets the list of enabled native plugins.
   * Fallback: returns empty array
   */
  getEnabledPlugins(): string[] {
    if (this.isNativeWrapper() && window.TransistorCore) {
      try {
        const json = window.TransistorCore.getEnabledPluginsJson();
        return JSON.parse(json);
      } catch (err) {
        console.error("Failed to parse native enabled plugins list:", err);
      }
    }
    this.triggerFallback("Running outside native wrapper. Falling back to empty plugin list.");
    return [];
  }
}

/**
 * Global helper class for custom plugins to easily handle native checks & fallbacks.
 */
export class WrapperPluginBridge {
  /**
   * Safe execution helper for plugin methods.
   * @param pluginName Name of the plugin injected on the window object (e.g. "TransistorCamera")
   * @param method Name of the method to call on that plugin object
   * @param args Arguments to pass
   * @param fallback Implementation to run if the native plugin is not present.
   */
  static callNative<T>(
    pluginName: string,
    method: string,
    args: any[],
    fallback: () => T
  ): T {
    if (typeof window !== "undefined" && window[pluginName] && typeof window[pluginName][method] === "function") {
      try {
        return window[pluginName][method](...args);
      } catch (err) {
        console.error(`Error calling native plugin method: ${pluginName}.${method}`, err);
      }
    }
    console.warn(`[TransistorWrapper SDK] Plugin ${pluginName} or method ${method} not found natively. Running web fallback.`);
    return fallback();
  }
}
