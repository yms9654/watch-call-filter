import {
  collection, doc, getDoc, getDocs, deleteDoc, setDoc, addDoc,
  query, orderBy, serverTimestamp, Timestamp, runTransaction,
} from 'firebase/firestore';
import { db } from './firebase';
import type { AllowlistEntry, Watch } from './types';

function tsToMillis(t: unknown): number {
  if (t instanceof Timestamp) return t.toMillis();
  if (typeof t === 'number') return t;
  return Date.now();
}

export async function listWatches(uid: string): Promise<Watch[]> {
  const snap = await getDocs(collection(db, 'users', uid, 'watches'));
  return snap.docs.map((d) => ({
    id: d.id,
    label: (d.data().label as string) ?? d.id.slice(0, 8),
    pairedAt: tsToMillis(d.data().pairedAt),
  }));
}

export async function listAllowlist(uid: string, watchId: string): Promise<AllowlistEntry[]> {
  const snap = await getDocs(query(
    collection(db, 'users', uid, 'watches', watchId, 'allowlist'),
    orderBy('name'),
  ));
  return snap.docs.map((d) => ({
    id: d.id,
    name: d.data().name as string,
    e164: d.data().e164 as string,
    addedAt: tsToMillis(d.data().addedAt),
  }));
}

export async function addAllowlistEntry(
  uid: string, watchId: string, name: string, e164: string,
): Promise<void> {
  await addDoc(collection(db, 'users', uid, 'watches', watchId, 'allowlist'), {
    name, e164, addedAt: serverTimestamp(),
  });
}

export async function deleteAllowlistEntry(
  uid: string, watchId: string, entryId: string,
): Promise<void> {
  await deleteDoc(doc(db, 'users', uid, 'watches', watchId, 'allowlist', entryId));
}

export async function claimPairingCode(uid: string, code: string, label: string): Promise<string> {
  const codeRef = doc(db, 'pairingCodes', code);
  return runTransaction(db, async (tx) => {
    const codeSnap = await tx.get(codeRef);
    if (!codeSnap.exists()) throw new Error('페어링 코드를 찾을 수 없습니다');
    const data = codeSnap.data();
    const expiresAt = tsToMillis(data.expiresAt);
    if (expiresAt < Date.now()) throw new Error('페어링 코드가 만료되었습니다');
    const watchId = data.watchId as string;
    const watchRef = doc(db, 'users', uid, 'watches', watchId);
    const existing = await tx.get(watchRef);
    if (existing.exists()) throw new Error('이미 등록된 워치입니다');
    tx.set(watchRef, { label, pairedAt: serverTimestamp(), watchId });
    tx.delete(codeRef);
    return watchId;
  });
}

export async function renameWatch(uid: string, watchId: string, label: string): Promise<void> {
  const ref = doc(db, 'users', uid, 'watches', watchId);
  await setDoc(ref, { label }, { merge: true });
}

export async function deleteWatch(uid: string, watchId: string): Promise<void> {
  // Best-effort: delete all allowlist entries first
  const entries = await getDocs(collection(db, 'users', uid, 'watches', watchId, 'allowlist'));
  await Promise.all(entries.docs.map((d) => deleteDoc(d.ref)));
  await deleteDoc(doc(db, 'users', uid, 'watches', watchId));
}

export async function getWatch(uid: string, watchId: string): Promise<Watch | null> {
  const snap = await getDoc(doc(db, 'users', uid, 'watches', watchId));
  if (!snap.exists()) return null;
  return {
    id: snap.id,
    label: (snap.data().label as string) ?? snap.id.slice(0, 8),
    pairedAt: tsToMillis(snap.data().pairedAt),
  };
}
