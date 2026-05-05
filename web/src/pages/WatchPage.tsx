import { useEffect, useState } from 'react';
import type { AllowlistEntry, BlockLogEntry, Watch } from '../lib/types';
import {
  addAllowlistEntry, deleteAllowlistEntry, getWatch, listAllowlist, listBlockLog,
} from '../lib/firestore';
import { formatKr, toE164Kr } from '../lib/phone';

interface Props { uid: string; watchId: string }

export function WatchPage({ watchId }: Props) {
  const [watch, setWatch] = useState<Watch | null>(null);
  const [entries, setEntries] = useState<AllowlistEntry[] | null>(null);
  const [blocks, setBlocks] = useState<BlockLogEntry[] | null>(null);
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    getWatch(watchId).then(setWatch).catch(console.error);
    reload();
    reloadBlocks();
  }, [watchId]);

  const reload = () => {
    setEntries(null);
    listAllowlist(watchId).then(setEntries).catch((e) => setError(e.message));
  };

  const reloadBlocks = () => {
    setBlocks(null);
    listBlockLog(watchId).then(setBlocks).catch((e) => setError(e.message));
  };

  const onAdd = async () => {
    setError(null);
    const e164 = toE164Kr(phone);
    if (!e164) { setError('올바른 번호를 입력해주세요'); return; }
    if (!name.trim()) { setError('이름을 입력해주세요'); return; }
    setBusy(true);
    try {
      await addAllowlistEntry(watchId, name.trim(), e164);
      setName(''); setPhone('');
      reload();
    } catch (e) { setError((e as Error).message); }
    finally { setBusy(false); }
  };

  const onDelete = async (entry: AllowlistEntry) => {
    if (!window.confirm(`"${entry.name}" 항목을 삭제할까요?`)) return;
    await deleteAllowlistEntry(watchId, entry.id);
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

      <section>
        <div className="flex items-baseline justify-between mb-3">
          <h2 className="font-semibold">차단 기록</h2>
          <button
            onClick={reloadBlocks}
            className="text-xs text-slate-500 hover:text-slate-900"
          >새로고침</button>
        </div>
        {blocks === null && <p className="text-sm text-slate-500">불러오는 중…</p>}
        {blocks?.length === 0 && (
          <p className="text-sm text-slate-500 rounded-2xl bg-white border border-slate-200 p-5">
            차단된 통화가 없습니다.
          </p>
        )}
        <ul className="space-y-2">
          {blocks?.map((b) => (
            <li key={b.id} className="rounded-xl bg-white border border-slate-200 p-4 flex items-center justify-between">
              <div>
                <div className="font-medium font-mono">
                  {b.e164 ? formatKr(b.e164) : <span className="italic text-slate-500">비공개 번호</span>}
                </div>
                <div className="text-xs text-slate-500">
                  {b.reason === 'private_number' ? '비공개 발신' : '주소록 미등록'}
                </div>
              </div>
              <div className="text-xs text-slate-500">{formatTime(b.blockedAt)}</div>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

function formatTime(ms: number): string {
  const d = new Date(ms);
  const now = new Date();
  const sameDay = d.toDateString() === now.toDateString();
  if (sameDay) return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
  return d.toLocaleString('ko-KR', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}
