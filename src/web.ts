import { WebPlugin } from '@capacitor/core';

import type { ZiliconNfcPlugin } from './definitions';

export class ZiliconNfcWeb extends WebPlugin implements ZiliconNfcPlugin {
  async startNfcSession(): Promise<void> {
    console.log('START NFC SESSION');
  }
}
