import { WrapperPluginBridge } from "@transistorjs/wrapper-core";

/**
 * Toast Plugin for Transistor Web Apps.
 */
export class ToastPlugin {
  /**
   * Shows a toast message natively if running in the wrapper app,
   * otherwise falls back to a web alert or custom web-based fallback.
   */
  static show(message: string, webFallback?: () => void): void {
    WrapperPluginBridge.callNative(
      "TransistorToast",
      "showToast",
      [message],
      () => {
        if (webFallback) {
          webFallback();
        } else {
          console.log(`[Web Fallback Toast]: ${message}`);
          if (typeof window !== "undefined") {
            alert(message);
          }
        }
      }
    );
  }
}
