import { WrapperPluginBridge } from "@transistorjs/wrapper-core";
import { TransistorAppItem } from "@transistorjs/wrapper-types";

export type AppItem = TransistorAppItem;

/**
 * Apps & Launcher Plugin for Transistor Web Apps.
 * Allows listing installed host applications, their icons, launching them, and requesting uninstallation.
 */
export class AppsPlugin {
  /**
   * Retrieves installed apps on the host device.
   * Runs native TransistorApps.getAppsJson() if available, otherwise executes webFallback.
   */
  static getApps(webFallback?: () => AppItem[]): AppItem[] {
    const raw = WrapperPluginBridge.callNative<string | AppItem[]>(
      "TransistorApps",
      "getAppsJson",
      [],
      () => {
        if (webFallback) {
          return webFallback();
        }
        console.warn("[Web Fallback AppsPlugin]: Returning mock web apps.");
        return [
          {
            packageName: "com.example.browser",
            appName: "Web Browser",
            icon: "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='64' height='64' viewBox='0 0 24 24' fill='none' stroke='%233b82f6' stroke-width='2'><circle cx='12' cy='12' r='10'/><path d='M2 12h20'/><path d='M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z'/></svg>",
            isSystemApp: true
          },
          {
            packageName: "com.example.camera",
            appName: "Camera",
            icon: "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='64' height='64' viewBox='0 0 24 24' fill='none' stroke='%23ef4444' stroke-width='2'><path d='M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z'/><circle cx='12' cy='13' r='3'/></svg>",
            isSystemApp: true
          },
          {
            packageName: "com.example.calculator",
            appName: "Calculator",
            icon: "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='64' height='64' viewBox='0 0 24 24' fill='none' stroke='%2310b981' stroke-width='2'><rect x='4' y='2' width='16' height='20' rx='2'/><line x1='8' y1='6' x2='16' y2='6'/><line x1='16' y1='14' x2='16' y2='18'/><path d='M16 10h.01'/><path d='M12 10h.01'/><path d='M8 10h.01'/><path d='M12 14h.01'/><path d='M8 14h.01'/><path d='M12 18h.01'/><path d='M8 18h.01'/></svg>",
            isSystemApp: false
          }
        ];
      }
    );

    if (typeof raw === "string") {
      try {
        return JSON.parse(raw);
      } catch (err) {
        console.error("Failed to parse JSON string from TransistorApps.getAppsJson()", err);
        return [];
      }
    }
    return Array.isArray(raw) ? raw : [];
  }

  /**
   * Launches an installed application by package name.
   */
  static launchApp(packageName: string, webFallback?: () => void): boolean {
    return WrapperPluginBridge.callNative<boolean>(
      "TransistorApps",
      "launchApp",
      [packageName],
      () => {
        if (webFallback) {
          webFallback();
        } else {
          console.log(`[Web Fallback AppsPlugin]: Launching app ${packageName}`);
          if (typeof window !== "undefined") {
            alert(`[Web Fallback] Launching app: ${packageName}`);
          }
        }
        return false;
      }
    );
  }

  /**
   * Requests uninstallation of an application by package name.
   */
  static uninstallApp(packageName: string, webFallback?: () => void): boolean {
    return WrapperPluginBridge.callNative<boolean>(
      "TransistorApps",
      "uninstallApp",
      [packageName],
      () => {
        if (webFallback) {
          webFallback();
        } else {
          console.log(`[Web Fallback AppsPlugin]: Requesting uninstall for app ${packageName}`);
          if (typeof window !== "undefined") {
            alert(`[Web Fallback] Requested uninstall for: ${packageName}`);
          }
        }
        return false;
      }
    );
  }
}
