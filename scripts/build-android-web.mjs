import { build } from 'vite';
import { resolve } from 'node:path';

// Bundle the same application and public images; never copy environment files.
await build({base: '/', build: {outDir: resolve('android/app/src/main/assets/web'), emptyOutDir: true}});
