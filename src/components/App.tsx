import { useCallback, useState } from "react";

import { Key } from "./Key";
import { Button } from "./Button";

import { Api } from "../utils/Api";

type KeyType = {
  id: string;
  value: string;
};

/*
  Легенда:
  Необходимо отобразить список ключей (произвольные строки), запрашиваемый с бэкенда.
  При нажатии кнопки "Сгенерировать ключ" необходимо сгенерировать новый ключ (апи запрос) и отобразить его в списке.
  Каждый ключ можно использовать. В таком случае он должен изменить свое визуальное отображение и увеличить счетчик использованных ключей.
  Каждый ключ можно удалить. При удалении использованного ключа, счетчик использованных ключей уменьшается (счетчику важны только неудаленные использованные ключи)

  Для поддержания актуального списка ключей необходимо обновлять список каждые 30 секунд. При получении от апи новых ключей считать их неиспользованными
 */

export const App = () => {
  const [keys, setKeys] = useState<any>(null);
  const [isKeysRequested, setIsKeysRequested] = useState<boolean>(false);

  const [isLoading, setLoadingState] = useState<boolean>(false);
  const [countUsedKeys, setCountUsedKeys] = useState<number>(0);

  const toggleLoading = useCallback(() => {
    setLoadingState(!isLoading);
  }, [isLoading]);

  const incrementUsedKeys = () => {
    setCountUsedKeys((prevValue) => prevValue++);
  }

  const decrementUsedKeys = useCallback(() => {
    setCountUsedKeys((prevValue) => prevValue--);
  }, []);

  const addKey = useCallback(async () => {
    toggleLoading();

    const key = await Api.generateKey();
    setKeys((prevKeys: KeyType[]) => prevKeys.push(key));

    toggleLoading();
  }, [toggleLoading]);

  const removeKey = useCallback((value: string) => {
    setKeys((prevKeys: KeyType[]) => prevKeys.filter((key) => key.value !== value));
  }, []);

  if (!isKeysRequested) {
    setIsKeysRequested(true);
    Api.loadKeys().then((response) => {
      setKeys(response);
      setInterval(() => {
        Api.loadKeys().then((response) => {
          setKeys(response);
        })
      }, 30000);
    }).catch((e: unknown) => {
      setIsKeysRequested(false);
    })
  }

  return (
    <main>
      <div>
        <h3>Всего ключей: {keys.length}</h3>
        <h3>Использовано текущих ключей: {countUsedKeys}</h3>
      </div>

      {!keys?.length && <div>Список ключей пуст</div>}

      {keys && keys.length > 0 && (
        <div className="keys">
          {keys.map((key: KeyType) => (
            <Key
              value={key.value}
              removeKey={removeKey(key.value)}
              decrementUsedKeys={decrementUsedKeys}
              incrementUsedKeys={incrementUsedKeys}
            />
          ))}
        </div>
      )}

      <div>
        <Button
          onClick={addKey}
          isLoading={isLoading}
        />
      </div>
    </main>
  );
};