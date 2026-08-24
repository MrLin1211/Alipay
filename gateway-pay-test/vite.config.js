import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';

export default defineConfig({
  build: {
    rollupOptions: {
      input: {
        main: fileURLToPath(new URL('./index.html', import.meta.url)),
        payResult: fileURLToPath(new URL('./pay-result.html', import.meta.url))
      }
    }
  }
});
