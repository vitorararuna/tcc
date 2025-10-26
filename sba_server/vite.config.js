// /sba_server/vite.config.js

import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';

export default defineConfig({
  plugins: [vue()],
  build: {
    // O diretório de saída que o Gradle espera.
    outDir: 'build/generated/js',
    emptyOutDir: true,
    // Configuração da biblioteca, espelhando o exemplo oficial.
    lib: {
      // Nosso ponto de entrada continua o mesmo.
      entry: path.resolve(__dirname, 'src/main/frontend/index.js'),
      // Usar o formato UMD, que é mais compatível.
      formats: ['umd'],
      // Um nome global para o módulo UMD.
      name: 'SbaCustomUi',
      // O nome do arquivo de saída.
      fileName: 'index',
    },
    rollupOptions: {
      external: ['vue'],
      output: {
        globals: {
          vue: 'Vue',
        },
      },
    },
  },
});