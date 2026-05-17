import { useAppStore } from '../../store/useAppStore'

export default function TemperatureInput() {
  const { temperature, setTemperature } = useAppStore()

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const raw = e.target.value
    if (raw === '') {
      setTemperature(null)
      return
    }
    const val = parseFloat(raw)
    if (!isNaN(val)) setTemperature(val)
  }

  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-xs text-gray-400 uppercase tracking-wider">Температура</label>
      <input
        type="number"
        min={0}
        max={1}
        step={0.1}
        value={temperature ?? ''}
        onChange={handleChange}
        placeholder="по умолчанию"
        className="bg-gray-800 border border-gray-700 rounded-md px-3 py-2 text-sm text-gray-100 placeholder-gray-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
      />
      <div className="text-xs text-gray-500 leading-relaxed">
        Диапазон: <span className="text-gray-400">0.0 – 1.0</span>
        <div className="mt-1 flex flex-col gap-0.5">
          <span><span className="text-blue-400">0.0</span> — точный, детерминированный</span>
          <span><span className="text-blue-400">0.7</span> — баланс точности и вариативности</span>
          <span><span className="text-blue-400">1.0</span> — максимально творческий</span>
        </div>
        <div className="mt-1 text-gray-600">Пусто = дефолт модели</div>
      </div>
    </div>
  )
}
