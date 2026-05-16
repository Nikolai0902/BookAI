import { useEffect, useRef, useState } from 'react'
import { useAgentStore } from '../../store/useAgentStore'
import { fetchProfiles, updateProfile } from '../../api/agentApi'
import type { UserProfile, CommunicationStyle, ResponseFormat } from '../../api/agentApi'

const STYLES: CommunicationStyle[] = ['FORMAL', 'CASUAL', 'TECHNICAL', 'CONCISE']
const FORMATS: ResponseFormat[] = ['PLAIN', 'MARKDOWN', 'BULLETS', 'DETAILED']

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
      <div className="bg-gray-900 border border-gray-700 rounded-xl shadow-2xl w-96 flex flex-col">
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-800">
          <h2 className="text-sm font-semibold text-white">Profile: {profile.displayName}</h2>
          <button
            onClick={onClose}
            className="text-gray-500 hover:text-white transition-colors text-lg leading-none"
          >
            ×
          </button>
        </div>

        <div className="flex flex-col gap-4 px-5 py-4 text-sm">
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
        </div>

        <div className="flex gap-2 justify-end px-5 py-4 border-t border-gray-800">
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
