import { registerPlugin } from '@capacitor/core';

import type { ZiliconNfcPlugin } from './definitions';

const ZiliconNfc = registerPlugin<ZiliconNfcPlugin>('ZiliconNfc', {
  web: () => import('./web').then(m => new m.ZiliconNfcWeb()),
});

export * from './definitions';
export { ZiliconNfc };
