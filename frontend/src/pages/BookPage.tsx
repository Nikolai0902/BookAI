import Sidebar from '../components/Sidebar/Sidebar'
import PromptInput from '../components/Chat/PromptInput'
import ResponseView from '../components/Chat/ResponseView'

export default function BookPage() {
  return (
    <div className="flex h-screen bg-gray-950 text-gray-100">
      <Sidebar />
      <main className="flex flex-col flex-1 overflow-hidden">
        <ResponseView />
        <PromptInput />
      </main>
    </div>
  )
}
