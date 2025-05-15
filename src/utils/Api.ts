const generateString = () => Math.random().toString(36).slice(2);
const sleep = async (delay = 0, callback) => setTimeout(callback, delay);

export class Api {
  static async loadKeys() {
    await sleep(200);
    return [];
  }

  static async generateKey() {
    await sleep(200);
    return generateString();
  }

  static async addUsedKey(_value) {
    await sleep(200);
    return true;
  }

  static async removeUsedKey(_value) {
    await sleep(200);
    return true;
  }
}
