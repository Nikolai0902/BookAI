import { useEffect, useState } from 'react'
import axios from 'axios'

export default function ModelInfo() {
  const [model, setModel] = useState<string | null>(null)

  useEffect(() => {
    axios.get<{ model: string }>('/api/info')
      .then((r) => setModel(r.data.model))
      .catch(() => setModel('unavailable'))
  }, [])

  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-xs text-gray-400 uppercase tracking-wider">Модель</label>
      <div className="bg-gray-800 border border-gray-700 rounded-md px-3 py-2 text-sm text-blue-300 font-mono truncate">
        {model ?? '…'}
      </div>
    </div>
  )
}
