import { useState } from 'react'
import reactLogo from './assets/react.svg'
const viteLogo = '/vite.svg'
import './App.css'
import 'leaflet/dist/leaflet.css';
import 'leaflet/dist/leaflet.css';
import MapComponent from './MapComponent';


function App() {
  const [count, setCount] = useState(0)

  return (
    <>
      <div>
        <a href="https://vite.dev" target="_blank">
          <img src={viteLogo} className="logo" alt="Vite logo" />
        </a>
        <a href="https://react.dev" target="_blank">
          <img src={reactLogo} className="logo react" alt="React logo" />
        </a>
      </div>
      <h1>Vite + React</h1>
      <div style={{ padding: '8px 16px', margin: '16px auto', maxWidth: '480px', backgroundColor: '#e8f5e9', color: '#1b5e20', border: '1px solid #a5d6a7', borderRadius: '8px', fontWeight: 'bold' }}>
        🚀 CI/CD Pipeline Active: Tekton + ArgoCD Verified
      </div>
      <div className="card">
        <button onClick={() => setCount((count) => count + 1)}>
          count is {count}
        </button>
        <p>
          Edit <code>src/App.tsx</code> and save to test HMR
        </p>
      </div>
      <p className="read-the-docs">
        Click on the Vite and React logos to learn more
      </p>

        <div>
            <h1>My Simple Map:</h1>
            <MapComponent />
        </div>

    </>
  )
}

export default App
