import { initializeApp } from 'firebase/app';
import { getAuth, GoogleAuthProvider } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';

const firebaseConfig = {
  projectId: 'watch-call-filter',
  appId: '1:774210837405:web:54af017c60b914b9feed74',
  storageBucket: 'watch-call-filter.firebasestorage.app',
  apiKey: 'AIzaSyBFwI1zBQImSzIZibNuFGr54Crae0Y1QvQ',
  authDomain: 'watch-call-filter.firebaseapp.com',
  messagingSenderId: '774210837405',
};

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
export const googleProvider = new GoogleAuthProvider();
