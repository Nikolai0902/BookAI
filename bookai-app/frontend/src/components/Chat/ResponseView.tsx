import { useAppStore } from '../../store/useAppStore'
import AskView from './AskView'
import CompareView from './CompareView'
import ReasonView from './ReasonView'

export default function ResponseView() {
  const { response, error, loading, mode } = useAppStore()

  return (
    <div className="flex-1 overflow-y-auto p-6">
      {error && (
        <div className="mb-4 px-4 py-3 bg-red-900/30 border border-red-700 rounded-lg text-red-400 text-sm">
          {error}
        </div>
      )}

      {loading && (
        <div className="flex gap-1 items-center text-gray-500 text-sm">
          <span>Генерирую ответ</span>
          <span className="animate-bounce">.</span>
          <span className="animate-bounce [animation-delay:150ms]">.</span>
          <span className="animate-bounce [animation-delay:300ms]">.</span>
        </div>
      )}

      {!loading && response && (
        mode === 'COMPARE' ? <CompareView answer={response} /> :
        mode === 'REASON'  ? <ReasonView answer={response} /> :
                             <AskView answer={response} />
      )}

      {!loading && !response && !error && (
        <div className="text-gray-600 text-sm">Ответ появится здесь…</div>
      )}
    </div>
  )
}
