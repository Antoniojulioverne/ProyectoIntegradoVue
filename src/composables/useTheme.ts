import { ref, onMounted } from 'vue';

const isDarkMode = ref(false);

export function useTheme() {
  const toggleDarkMode = () => {
    isDarkMode.value = !isDarkMode.value;
  };

  const setupDarkModeListener = () => {
    if (window.matchMedia) {
      const darkModeQuery = window.matchMedia('(prefers-color-scheme: dark)');

      // Valor inicial
      isDarkMode.value = darkModeQuery.matches;

      const updateDarkMode = (event: MediaQueryListEvent) => {
        isDarkMode.value = event.matches;
      };

      if (darkModeQuery.addEventListener) {
        darkModeQuery.addEventListener('change', updateDarkMode);
      } else if (darkModeQuery.addListener) {
        darkModeQuery.addListener(updateDarkMode);
      }

      // Retorna función cleanup
      return () => {
        if (darkModeQuery.removeEventListener) {
          darkModeQuery.removeEventListener('change', updateDarkMode);
        } else if (darkModeQuery.removeListener) {
          darkModeQuery.removeListener(updateDarkMode);
        }
      };
    }
  };

  onMounted(() => {
    setupDarkModeListener();
  });

  return {
    isDarkMode,
    toggleDarkMode,
    setupDarkModeListener
  };
}
