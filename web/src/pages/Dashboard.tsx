import { useEffect, useState } from 'react';
import type { Watch } from '../lib/types';
import { claimPairingCode, deleteWatch, listWatches, renameWatch } from '../lib/firestore';

interface Props { uid: string }

export function Dashboard({ uid }: Props) {
  const [watches, setWatches] = useState<Watch[] | null>(null);
  const [code, setCode] = useState('');
  const [label, setLabel] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reload = () => {
    setWatches(null);
    listWatches(uid).then(setWatches).catch((e) => setError(e.message));
  };

  useEffect(reload, [uid]);

  const onClaim = async () => {
    setError(null); setBusy(true);
    try {
      await claimPairingCode(uid, code.trim(), label.trim() || '내 워치');
      setCode(''); setLabel('');
      reload();
    } catch (e) { setError((e as Error).message); }
    finally { setBusy(false); }
  };

  const onRename = async (w: Watch) => {
    const next = window.prompt('새 이름', w.label);
    if (!next || next === w.label) return;
    await renameWatch(w.id, next.trim());
    reload();
  };

  const onDelete = async (w: Watch) => {
    if (!window.confirm(`"${w.label}" 워치 등록을 해제할까요?\n허용 번호도 함께 삭제됩니다.`)) return;
    await deleteWatch(w.id);
    reload();
  };

  return (
    <div className="space-y-6">
      <section className="rounded-2xl bg-white border border-slate-200 p-5">
        <h2 className="font-semibold mb-3">새 워치 등록</h2>
        <p className="text-sm text-slate-500 mb-3">
          워치 앱 화면에 표시된 6자리 페어링 코드를 입력하세요.
        </p>
        <div className="flex flex-col sm:flex-row gap-2">
          <input
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
            placeholder="000000"
            inputMode="numeric"
            className="rounded-lg border border-slate-300 px-3 py-2 w-32 tracking-widest text-center font-mono"
          />
          <input
            value={label}
            onChange={(e) => setLabel(e.target.value.slice(0, 30))}
            placeholder="이름 (선택, 예: 첫째 워치)"
            className="rounded-lg border border-slate-300 px-3 py-2 flex-1"
          />
          <button
            onClick={onClaim}
            disabled={busy || code.length !== 6}
            className="rounded-lg bg-slate-900 px-4 py-2 text-white font-medium disabled:opacity-40"
          >등록</button>
        </div>
        {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
      </section>

      <section>
        <h2 className="font-semibold mb-3">내 워치</h2>
        {watches === null && <p className="text-sm text-slate-500">불러오는 중…</p>}
        {watches?.length === 0 && (
          <p className="text-sm text-slate-500 rounded-2xl bg-white border border-slate-200 p-5">
            등록된 워치가 없습니다. 위에서 페어링 코드로 추가하세요.
          </p>
        )}
        <ul className="space-y-2">
          {watches?.map((w) => (
            <li key={w.id} className="rounded-xl bg-white border border-slate-200 p-4 flex items-center justify-between">
              <a href={`#/watch/${w.id}`} className="flex-1">
                <div className="font-medium">{w.label}</div>
                <div className="text-xs text-slate-500 font-mono">{w.id}</div>
              </a>
              <div className="flex gap-3 text-sm">
                <button onClick={() => onRename(w)} className="text-slate-600 hover:text-slate-900">이름 변경</button>
                <button onClick={() => onDelete(w)} className="text-red-600 hover:text-red-700">삭제</button>
              </div>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
