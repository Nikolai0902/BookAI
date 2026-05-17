import { BrowserRouter, Routes, Route } from 'react-router-dom'
import BookPage from './pages/BookPage'
import AgentPage from './pages/AgentPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<BookPage />} />
        <Route path="/agent" element={<AgentPage />} />
      </Routes>
    </BrowserRouter>
  )
}
