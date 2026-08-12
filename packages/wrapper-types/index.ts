/**
 * Interface representing the native Core Plugin injected by the Android app.
 */
export interface TransistorCoreInterface {
  /**
   * Returns the Android application's version name (e.g. "1.0.0").
   */
  getAppVersion(): string;

  /**
   * Returns a JSON-stringified array of enabled plugin names.
   * Returning a JSON string is extremely reliable for WebView Javascript interfaces.
   */
  getEnabledPluginsJson(): string;
}

/**
 * Interface representing an installed application on the host device.
 */
export interface TransistorAppItem {
  packageName: string;
  appName: string;
  icon: string;
  isSystemApp?: boolean;
}

/**
 * Interface representing the native Apps / Launcher Plugin injected by the Android app.
 */
export interface TransistorAppsInterface {
  /**
   * Returns a JSON string of installed applications on the device.
   */
  getAppsJson(): string;

  /**
   * Launches the specified application natively by its package name.
   */
  launchApp(packageName: string): boolean;

  /**
   * Triggers the Android native system uninstallation dialog for the package name.
   */
  uninstallApp(packageName: string): boolean;
}

declare global {
  interface Window {
    TransistorCore?: TransistorCoreInterface;
    TransistorApps?: TransistorAppsInterface;
    [key: string]: any;
  }
}
