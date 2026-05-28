import { BrowserRouter, Routes, Route } from 'react-router-dom'
import BookPage from './pages/BookPage'
import AgentPage from './pages/AgentPage'
import RagChatPage from './pages/RagChatPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<BookPage />} />
        <Route path="/agent" element={<AgentPage />} />
        <Route path="/rag-chat" element={<RagChatPage />} />
      </Routes>
    </BrowserRouter>
  )
}
