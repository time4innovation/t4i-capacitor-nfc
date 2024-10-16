import { PluginListenerHandle } from "@capacitor/core";

export interface ZiliconNfcPlugin {
  /**
   * Starts the NFC Session
   */
  startNfcSession(): Promise<void>;

  /**
   * Add a listener for the NFC detected event.
   */
  addListener(
    eventName: 'nfcDetected',
    listenerFunc: (messages: { messages: Array<{ records: Array<{ type: number, identifier: string, payload: string }> }> }) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
  * Removes all listeners
  */
  removeAllListeners(): Promise<void>;
}
