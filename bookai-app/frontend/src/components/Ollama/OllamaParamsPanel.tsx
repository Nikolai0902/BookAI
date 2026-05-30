import { useOllamaChatStore, DEFAULT_PARAMS, RAG_SYSTEM_PROMPT } from '../../store/useOllamaChatStore'

const NUM_CTX_VALUES = [1024, 2048, 4096, 8192]

export default function OllamaParamsPanel() {
  const params = useOllamaChatStore((s) => s.params)
  const setParams = useOllamaChatStore((s) => s.setParams)
  const setOptions = useOllamaChatStore((s) => s.setOptions)
  const resetParams = useOllamaChatStore((s) => s.resetParams)
  const modelInfo = useOllamaChatStore((s) => s.modelInfo)

  const opt = params.options
  const effectiveModel = params.model || modelInfo?.model || 'qwen2.5:3b'

  const onPresetChange = (preset: 'default' | 'rag') => {
    setParams({
      promptPreset: preset,
      systemPrompt: preset === 'rag' ? RAG_SYSTEM_PROMPT : '',
    })
  }

  return (
    <div className="flex flex-col gap-3 p-3 bg-gray-800 rounded-lg border border-gray-700 text-xs">
      <div className="flex items-center justify-between">
        <div className="text-gray-400 uppercase tracking-wider font-medium">Параметры</div>
        <button
          onClick={resetParams}
          className="text-[10px] px-2 py-1 rounded bg-gray-700 hover:bg-gray-600 text-gray-300"
        >
          Сброс
        </button>
      </div>

      <label className="flex flex-col gap-1">
        <span className="text-gray-400">Модель</span>
        <input
          type="text"
          value={params.model}
          onChange={(e) => setParams({ model: e.target.value })}
          placeholder={modelInfo?.model || 'qwen2.5:3b'}
          className="bg-gray-900 border border-gray-700 rounded px-2 py-1 text-gray-100 placeholder-gray-600 focus:outline-none focus:border-purple-600"
        />
        <span className="text-[10px] text-gray-600">Пусто → берётся из конфига сервера</span>
      </label>

      <label className="flex flex-col gap-1">
        <span className="flex justify-between text-gray-400">
          <span>temperature</span>
          <span className="text-gray-200 font-mono">{(opt.temperature ?? 0).toFixed(2)}</span>
        </span>
        <input
          type="range" min={0} max={1.5} step={0.05}
          value={opt.temperature ?? 0}
          onChange={(e) => setOptions({ temperature: parseFloat(e.target.value) })}
        />
      </label>

      <label className="flex flex-col gap-1">
        <span className="flex justify-between text-gray-400">
          <span>top_p</span>
          <span className="text-gray-200 font-mono">{(opt.topP ?? 0).toFixed(2)}</span>
        </span>
        <input
          type="range" min={0} max={1} step={0.05}
          value={opt.topP ?? 0}
          onChange={(e) => setOptions({ topP: parseFloat(e.target.value) })}
        />
      </label>

      <label className="flex flex-col gap-1">
        <span className="flex justify-between text-gray-400">
          <span>top_k</span>
          <span className="text-gray-200 font-mono">{opt.topK ?? 0}</span>
        </span>
        <input
          type="number" min={0} max={100} step={1}
          value={opt.topK ?? 0}
          onChange={(e) => setOptions({ topK: parseInt(e.target.value, 10) || 0 })}
          className="bg-gray-900 border border-gray-700 rounded px-2 py-1 text-gray-100"
        />
      </label>

      <label className="flex flex-col gap-1">
        <span className="text-gray-400">num_ctx</span>
        <select
          value={opt.numCtx ?? 4096}
          onChange={(e) => setOptions({ numCtx: parseInt(e.target.value, 10) })}
          className="bg-gray-900 border border-gray-700 rounded px-2 py-1 text-gray-100"
        >
          {NUM_CTX_VALUES.map((v) => (
            <option key={v} value={v}>{v}</option>
          ))}
        </select>
      </label>

      <label className="flex flex-col gap-1">
        <span className="flex justify-between text-gray-400">
          <span>num_predict</span>
          <span className="text-gray-200 font-mono">{opt.numPredict ?? 0}</span>
        </span>
        <input
          type="number" min={64} max={2048} step={32}
          value={opt.numPredict ?? 0}
          onChange={(e) => setOptions({ numPredict: parseInt(e.target.value, 10) || 0 })}
          className="bg-gray-900 border border-gray-700 rounded px-2 py-1 text-gray-100"
        />
      </label>

      <label className="flex flex-col gap-1">
        <span className="flex justify-between text-gray-400">
          <span>repeat_penalty</span>
          <span className="text-gray-200 font-mono">{(opt.repeatPenalty ?? 1).toFixed(2)}</span>
        </span>
        <input
          type="range" min={1} max={1.5} step={0.05}
          value={opt.repeatPenalty ?? 1}
          onChange={(e) => setOptions({ repeatPenalty: parseFloat(e.target.value) })}
        />
      </label>

      <div className="flex flex-col gap-1">
        <span className="text-gray-400">Системный промпт</span>
        <div className="flex gap-1">
          <button
            onClick={() => onPresetChange('default')}
            className={`flex-1 px-2 py-1 rounded text-[11px] ${
              params.promptPreset === 'default'
                ? 'bg-purple-700 text-white'
                : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
            }`}
          >
            Общий
          </button>
          <button
            onClick={() => onPresetChange('rag')}
            className={`flex-1 px-2 py-1 rounded text-[11px] ${
              params.promptPreset === 'rag'
                ? 'bg-purple-700 text-white'
                : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
            }`}
          >
            RAG-формат
          </button>
        </div>
        {params.promptPreset === 'rag' && (
          <p className="text-[10px] text-gray-500 leading-snug">
            Модель будет просить цитаты [N] и блок «Источники:».
          </p>
        )}
      </div>

      <div className="text-[10px] text-gray-600 leading-snug border-t border-gray-700 pt-2">
        Параметры применяются к следующему вопросу. Снимок сохраняется в каждое сообщение.
        <br />
        Дефолты: temp={DEFAULT_PARAMS.options.temperature}, ctx={DEFAULT_PARAMS.options.numCtx},
        predict={DEFAULT_PARAMS.options.numPredict}.
        <br />
        Активная модель: <span className="text-purple-400">{effectiveModel}</span>
      </div>
    </div>
  )
}
