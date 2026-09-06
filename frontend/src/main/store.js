import Store from 'electron-store';

let userId = null;

const store = new Store();

const initUserId = (_userId) => {
  userId = _userId;
};

const getUserId = () => {
  return userId;
};

const setData = (key, value) => {
  store.set(key, value);
};

const getData = (key) => {
  return store.get(key);
};

const setUserData = (key, value) => {
  store.set(userId + key, value);
};

const getUserData = (key) => {
  return store.get(userId + key);
};

const clearUserData = (key) => {
  store.delete(userId + key);
};



export {

  initUserId,
  getUserId,
  setData,
  getData,
  setUserData,
  getUserData,
  clearUserData,

};
