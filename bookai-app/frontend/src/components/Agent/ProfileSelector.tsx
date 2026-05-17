import { useEffect, useRef, useState } from 'react'
import { useAgentStore } from '../../store/useAgentStore'
import { fetchProfiles, updateProfile, getInvariants, addInvariant, deleteInvariant } from '../../api/agentApi'
import type { UserProfile, CommunicationStyle, ResponseFormat, AgentInvariant } from '../../api/agentApi'

const STYLES: CommunicationStyle[] = ['FORMAL', 'CASUAL', 'TECHNICAL', 'CONCISE']
const FORMATS: ResponseFormat[] = ['PLAIN', 'MARKDOWN', 'BULLETS', 'DETAILED']
const INVARIANT_CATEGORIES = ['архитектура', 'стек', 'бизнес', 'технические']

const STYLE_LABELS: Record<CommunicationStyle, string> = {
  FORMAL: 'Formal',
  CASUAL: 'Casual',
  TECHNICAL: 'Technical',
  CONCISE: 'Concise',
}

const FORMAT_LABELS: Record<ResponseFormat, string> = {
  PLAIN: 'Plain text',
  MARKDOWN: 'Markdown',
  BULLETS: 'Bullet points',
  DETAILED: 'Detailed',
}

function InvariantsSection({ profileId }: { profileId: string }) {
  const [invariants, setInvariants] = useState<AgentInvariant[]>([])
  const [adding, setAdding] = useState(false)
  const [newCategory, setNewCategory] = useState(INVARIANT_CATEGORIES[0])
  const [newRule, setNewRule] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    getInvariants(profileId).then(setInvariants).catch(console.error)
  }, [profileId])

  const byCategory = invariants.reduce<Record<string, AgentInvariant[]>>((acc, inv) => {
    ;(acc[inv.category] ??= []).push(inv)
    return acc
  }, {})

  const handleAdd = async () => {
    if (!newRule.trim()) return
    setSaving(true)
    try {
      const created = await addInvariant(profileId, { category: newCategory, ruleText: newRule.trim() })
      setInvariants((prev) => [...prev, created])
      setNewRule('')
      setAdding(false)
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: number) => {
    await deleteInvariant(profileId, id)
    setInvariants((prev) => prev.filter((i) => i.id !== id))
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center justify-between">
        <label className="text-xs text-gray-400 uppercase tracking-wider">Invariants</label>
        <button
          onClick={() => setAdding((v) => !v)}
          className="text-xs text-blue-400 hover:text-blue-300 transition-colors"
        >
          {adding ? 'Cancel' : '+ Add'}
        </button>
      </div>

      {adding && (
        <div className="flex flex-col gap-2 p-3 bg-gray-800 rounded-md border border-gray-700">
          <select
            value={newCategory}
            onChange={(e) => setNewCategory(e.target.value)}
            className="bg-gray-700 border border-gray-600 rounded px-2 py-1.5 text-xs text-gray-100 focus:outline-none focus:border-blue-500"
          >
            {INVARIANT_CATEGORIES.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
          <textarea
            value={newRule}
            onChange={(e) => setNewRule(e.target.value)}
            rows={2}
            placeholder="Текст правила..."
            className="bg-gray-700 border border-gray-600 rounded px-2 py-1.5 text-xs text-gray-100 placeholder-gray-500 focus:outline-none focus:border-blue-500 resize-none"
          />
          <button
            onClick={handleAdd}
            disabled={saving || !newRule.trim()}
            className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 rounded text-xs text-white disabled:opacity-50 transition-colors"
          >
            {saving ? 'Saving...' : 'Save'}
          </button>
        </div>
      )}

      {Object.keys(byCategory).length === 0 && !adding && (
        <p className="text-xs text-gray-600 italic">Нет инвариантов</p>
      )}

      {Object.entries(byCategory).map(([cat, items]) => (
        <div key={cat}>
          <div className="text-[10px] text-gray-500 uppercase tracking-wider mb-1">[{cat}]</div>
          {items.map((inv) => (
            <div key={inv.id} className="flex items-start gap-2 group py-0.5">
              <span className="text-xs text-gray-300 flex-1 leading-relaxed">{inv.ruleText}</span>
              <button
                onClick={() => handleDelete(inv.id)}
                className="text-gray-600 hover:text-red-400 transition-colors opacity-0 group-hover:opacity-100 shrink-0 text-xs mt-0.5"
                title="Удалить инвариант"
              >
                🗑
              </button>
            </div>
          ))}
        </div>
      ))}
    </div>
  )
}

function ProfileModal({
  profile,
  onClose,
  onSave,
}: {
  profile: UserProfile
  onClose: () => void
  onSave: (updated: UserProfile) => Promise<void>
}) {
  const [draft, setDraft] = useState<UserProfile>({ ...profile })
  const [saving, setSaving] = useState(false)
  const backdropRef = useRef<HTMLDivElement>(null)

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === backdropRef.current) onClose()
  }

  const handleSave = async () => {
    setSaving(true)
    try {
      await onSave(draft)
      onClose()
    } finally {
      setSaving(false)
    }
  }

  return (
    <div
      ref={backdropRef}
      onClick={handleBackdropClick}
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
    >
      <div className="bg-gray-900 border border-gray-700 rounded-xl shadow-2xl w-96 flex flex-col max-h-[90vh]">
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-800 shrink-0">
          <h2 className="text-sm font-semibold text-white">Profile: {profile.displayName}</h2>
          <button
            onClick={onClose}
            className="text-gray-500 hover:text-white transition-colors text-lg leading-none"
          >
            ×
          </button>
        </div>

        <div className="flex flex-col gap-4 px-5 py-4 text-sm overflow-y-auto">
          <div className="flex flex-col gap-1">
            <label className="text-xs text-gray-400 uppercase tracking-wider">Style</label>
            <select
              value={draft.style}
              onChange={(e) => setDraft({ ...draft, style: e.target.value as CommunicationStyle })}
              className="bg-gray-800 border border-gray-700 rounded-md px-3 py-2 text-gray-100 focus:outline-none focus:border-blue-500"
            >
              {STYLES.map((s) => (
                <option key={s} value={s}>{STYLE_LABELS[s]}</option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-xs text-gray-400 uppercase tracking-wider">Response format</label>
            <select
              value={draft.responseFormat}
              onChange={(e) => setDraft({ ...draft, responseFormat: e.target.value as ResponseFormat })}
              className="bg-gray-800 border border-gray-700 rounded-md px-3 py-2 text-gray-100 focus:outline-none focus:border-blue-500"
            >
              {FORMATS.map((f) => (
                <option key={f} value={f}>{FORMAT_LABELS[f]}</option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-xs text-gray-400 uppercase tracking-wider">Constraints</label>
            <textarea
              value={draft.constraints ?? ''}
              onChange={(e) => setDraft({ ...draft, constraints: e.target.value || null })}
              rows={3}
              placeholder="Any special instructions..."
              className="bg-gray-800 border border-gray-700 rounded-md px-3 py-2 text-gray-100 placeholder-gray-600 focus:outline-none focus:border-blue-500 resize-none"
            />
          </div>

          <div className="border-t border-gray-800 pt-3">
            <InvariantsSection profileId={profile.profileId} />
          </div>
        </div>

        <div className="flex gap-2 justify-end px-5 py-4 border-t border-gray-800 shrink-0">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm text-gray-400 hover:text-white transition-colors rounded-md hover:bg-gray-800"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            disabled={saving}
            className="px-4 py-2 text-sm bg-blue-600 hover:bg-blue-700 rounded-md text-white disabled:opacity-50 transition-colors"
          >
            {saving ? 'Saving...' : 'Save'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default function ProfileSelector() {
  const profileId = useAgentStore((s) => s.profileId)
  const setProfileId = useAgentStore((s) => s.setProfileId)
  const clearSession = useAgentStore((s) => s.clearSession)

  const [profiles, setProfiles] = useState<UserProfile[]>([])
  const [modalOpen, setModalOpen] = useState(false)

  useEffect(() => {
    fetchProfiles().then(setProfiles).catch(console.error)
  }, [])

  const active = profiles.find((p) => p.profileId === profileId)

  const handleSwitch = (newId: string) => {
    if (newId !== profileId) {
      setProfileId(newId)
      clearSession()
    }
  }

  const handleSave = async (updated: UserProfile) => {
    const saved = await updateProfile(updated.profileId, updated)
    setProfiles((prev) => prev.map((p) => p.profileId === saved.profileId ? saved : p))
  }

  return (
    <>
      <div className="flex flex-col gap-1.5">
        <label className="text-xs text-gray-400 uppercase tracking-wider">Profile</label>
        <div className="flex gap-1.5">
          <select
            value={profileId}
            onChange={(e) => handleSwitch(e.target.value)}
            className="flex-1 bg-gray-800 border border-gray-700 rounded-md px-3 py-2 text-sm text-gray-100 focus:outline-none focus:border-blue-500"
          >
            {profiles.map((p) => (
              <option key={p.profileId} value={p.profileId}>{p.displayName}</option>
            ))}
          </select>
          {active && (
            <button
              onClick={() => setModalOpen(true)}
              title="Edit profile preferences"
              className="px-2.5 py-2 bg-gray-800 border border-gray-700 rounded-md text-gray-400 hover:text-white hover:border-gray-500 transition-colors text-sm"
            >
              ⚙
            </button>
          )}
        </div>
      </div>

      {modalOpen && active && (
        <ProfileModal
          profile={active}
          onClose={() => setModalOpen(false)}
          onSave={handleSave}
        />
      )}
    </>
  )
}
