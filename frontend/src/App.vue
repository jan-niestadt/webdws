<!--
  Main Application Component - WebDWS XML Editor
  
  This is the root component that provides:
  - Application layout with header and navigation
  - Router integration for single-page application routing
  - Global styling and theme management
  - Main content area for the XML editor interface
-->
<template>
  <div id="app">
    <header class="header">
      <h1>WebDWS - XML Editor</h1>
      <nav>
        <router-link to="/" class="nav-link">Editor</router-link>
      </nav>
    </header>
    <main class="main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
// App component logic
</script>

<style scoped>
.header {
  background: #2c3e50;
  color: white;
  padding: 1rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.header h1 {
  margin: 0;
  font-size: 1.5rem;
}

.nav-link {
  color: white;
  text-decoration: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.nav-link:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.main {
  flex: 1;
  padding: 2rem;
}
</style>
