import { useEffect, useState } from 'react';
import type { AllowlistEntry, Watch } from '../lib/types';
import {
  addAllowlistEntry, deleteAllowlistEntry, getWatch, listAllowlist,
} from '../lib/firestore';
import { formatKr, toE164Kr } from '../lib/phone';

interface Props { uid: string; watchId: string }

export function WatchPage({ uid, watchId }: Props) {
  const [watch, setWatch] = useState<Watch | null>(null);
  const [entries, setEntries] = useState<AllowlistEntry[] | null>(null);
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    getWatch(uid, watchId).then(setWatch).catch(console.error);
    reload();
  }, [uid, watchId]);

  const reload = () => {
    setEntries(null);
    listAllowlist(uid, watchId).then(setEntries).catch((e) => setError(e.message));
  };

  const onAdd = async () => {
    setError(null);
    const e164 = toE164Kr(phone);
    if (!e164) { setError('올바른 번호를 입력해주세요'); return; }
    if (!name.trim()) { setError('이름을 입력해주세요'); return; }
    setBusy(true);
    try {
      await addAllowlistEntry(uid, watchId, name.trim(), e164);
      setName(''); setPhone('');
      reload();
    } catch (e) { setError((e as Error).message); }
    finally { setBusy(false); }
  };

  const onDelete = async (entry: AllowlistEntry) => {
    if (!window.confirm(`"${entry.name}" 항목을 삭제할까요?`)) return;
    await deleteAllowlistEntry(uid, watchId, entry.id);
    reload();
  };

  return (
    <div className="space-y-6">
      <a href="#/" className="text-sm text-slate-500 hover:text-slate-900">&larr; 돌아가기</a>

      <section>
        <h1 className="text-xl font-semibold">{watch?.label ?? '워치'}</h1>
        <p className="text-xs text-slate-500 font-mono mt-1">{watchId}</p>
      </section>

      <section className="rounded-2xl bg-white border border-slate-200 p-5">
        <h2 className="font-semibold mb-3">허용 번호 추가</h2>
        <div className="flex flex-col sm:flex-row gap-2">
          <input
            value={name}
            onChange={(e) => setName(e.target.value.slice(0, 40))}
            placeholder="이름"
            className="rounded-lg border border-slate-300 px-3 py-2 flex-1"
          />
          <input
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="010-1234-5678"
            inputMode="tel"
            className="rounded-lg border border-slate-300 px-3 py-2 flex-1"
          />
          <button
            onClick={onAdd}
            disabled={busy}
            className="rounded-lg bg-slate-900 px-4 py-2 text-white font-medium disabled:opacity-40"
          >추가</button>
        </div>
        {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
      </section>

      <section>
        <h2 className="font-semibold mb-3">허용된 번호</h2>
        {entries === null && <p className="text-sm text-slate-500">불러오는 중…</p>}
        {entries?.length === 0 && (
          <p className="text-sm text-slate-500 rounded-2xl bg-white border border-slate-200 p-5">
            아직 추가된 번호가 없습니다.
          </p>
        )}
        <ul className="space-y-2">
          {entries?.map((entry) => (
            <li key={entry.id} className="rounded-xl bg-white border border-slate-200 p-4 flex items-center justify-between">
              <div>
                <div className="font-medium">{entry.name}</div>
                <div className="text-sm text-slate-500 font-mono">{formatKr(entry.e164)}</div>
              </div>
              <button onClick={() => onDelete(entry)} className="text-sm text-red-600 hover:text-red-700">삭제</button>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
