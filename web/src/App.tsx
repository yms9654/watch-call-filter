import { useEffect, useState } from 'react';
import { onAuthStateChanged, signInWithPopup, signOut, type User } from 'firebase/auth';
import { auth, googleProvider } from './lib/firebase';
import { Dashboard } from './pages/Dashboard';
import { WatchPage } from './pages/WatchPage';

type Route = { name: 'dashboard' } | { name: 'watch'; watchId: string };

function parseRoute(): Route {
  const m = window.location.hash.match(/^#\/watch\/(.+)$/);
  if (m) return { name: 'watch', watchId: m[1] };
  return { name: 'dashboard' };
}

export function App() {
  const [user, setUser] = useState<User | null | undefined>(undefined);
  const [route, setRoute] = useState<Route>(parseRoute());

  useEffect(() => onAuthStateChanged(auth, setUser), []);

  useEffect(() => {
    const onHash = () => setRoute(parseRoute());
    window.addEventListener('hashchange', onHash);
    return () => window.removeEventListener('hashchange', onHash);
  }, []);

  if (user === undefined) {
    return <div className="grid h-full place-items-center text-slate-500">로딩 중…</div>;
  }

  if (user === null) {
    return (
      <div className="grid h-full place-items-center">
        <div className="rounded-2xl bg-white p-8 shadow-sm border border-slate-200 max-w-sm w-full text-center">
          <h1 className="text-xl font-semibold mb-2">Watch Call Filter</h1>
          <p className="text-sm text-slate-500 mb-6">자녀 워치의 허용 번호를 관리하세요.</p>
          <button
            onClick={() => signInWithPopup(auth, googleProvider).catch(console.error)}
            className="w-full rounded-lg bg-slate-900 px-4 py-2 text-white font-medium hover:bg-slate-800"
          >
            Google로 로그인
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-full">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto max-w-3xl flex items-center justify-between px-4 h-14">
          <a href="#/" className="font-semibold">Watch Call Filter</a>
          <div className="flex items-center gap-3 text-sm">
            <span className="text-slate-500 hidden sm:inline">{user.email}</span>
            <button
              onClick={() => signOut(auth)}
              className="text-slate-600 hover:text-slate-900"
            >로그아웃</button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-3xl px-4 py-6">
        {route.name === 'dashboard' && <Dashboard uid={user.uid} />}
        {route.name === 'watch' && <WatchPage uid={user.uid} watchId={route.watchId} />}
      </main>
    </div>
  );
}
