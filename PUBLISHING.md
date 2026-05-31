# Публикация в Maven Central через Sonatype Central Portal

Одноразовый setup (выполнить один раз перед первой публикацией) + рутинный workflow.

## 1. Регистрация и подтверждение namespace

1. Зарегистрироваться на https://central.sonatype.com (Sign In → войти через GitHub).
2. После входа, в `View Namespaces` подтвердить namespace `io.github.millersa`. Если GitHub-логин совпадает с `millersa`, namespace подтверждается **автоматически** при первой попытке публикации.
3. Сгенерировать **User Token** в разделе `Account` → `Generate User Token`. Сохранить полученные `<username>` и `<password>` — они нужны для `~/.m2/settings.xml`.

## 2. Настройка GPG-ключа

Central требует подписи всех артефактов (jar, sources-jar, javadoc-jar, pom).

```bash
# Сгенерировать ключ (один раз)
gpg --full-generate-key
# Тип: RSA and RSA, размер: 4096, срок: 0 (не истекает) или 2y
# Имя: millersa
# Email: melnik87@gmail.com
# Парольная фраза: запомнить или сохранить в безопасном месте

# Посмотреть свой fingerprint
gpg --list-secret-keys --keyid-format=long
# Вывод: sec   rsa4096/<KEY_ID>

# Опубликовать публичный ключ на keyserver (обязательно!)
gpg --keyserver keys.openpgp.org --send-keys <KEY_ID>
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
```

После загрузки на `keys.openpgp.org` нужно подтвердить email — придёт ссылка на `melnik87@gmail.com`.

## 3. Настройка `~/.m2/settings.xml`

Добавить блок `<server>` с credentials от Sonatype:

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>YOUR_SONATYPE_USERNAME</username>
      <password>YOUR_SONATYPE_TOKEN</password>
    </server>
  </servers>

  <!-- Опционально: парольная фраза GPG, чтобы не вводить руками -->
  <profiles>
    <profile>
      <id>gpg</id>
      <properties>
        <gpg.passphrase>YOUR_GPG_PASSPHRASE</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>gpg</activeProfile>
  </activeProfiles>
</settings>
```

## 4. SNAPSHOT vs Release

- **SNAPSHOT-версии** (`1.0.0-SNAPSHOT`) в Maven Central **не публикуются**. Для SNAPSHOT либо использовать GitHub Packages, либо `mvn install` локально.
- В Central можно опубликовать только **release**-версии (`1.0.0`, `1.0.1`, …).

Сейчас в pom.xml версия `1.0.0-SNAPSHOT`. Перед первой публикацией в Central поменять на `1.0.0`:

```bash
mvn versions:set -DnewVersion=1.0.0
mvn versions:commit  # удаляет backup
```

## 5. Сборка и публикация (рутина)

```bash
# Проверить локально (без подписи)
mvn clean install

# Запустить тесты
mvn test

# Сборка с подписью + публикация в Central через release-профиль
mvn -P release clean deploy
```

После `mvn deploy`:
1. Артефакт загружается в Sonatype Central staging.
2. С `autoPublish=true` Central автоматически прогоняет валидацию и публикует.
3. С `waitUntil=published` Maven дождётся появления артефакта (10-30 минут до синхронизации с Maven Central).

После публикации артефакт доступен по адресу:
```
https://repo1.maven.org/maven2/io/github/millersa/listing-toolkit/1.0.0/
```

## 6. Следующая версия

```bash
git tag v1.0.0
git push origin v1.0.0

# Поднять версию для разработки
mvn versions:set -DnewVersion=1.0.1-SNAPSHOT
mvn versions:commit
git commit -am "bump: 1.0.1-SNAPSHOT"
```

## Troubleshooting

- **"401 Unauthorized"** — проверить `<server id="central">` в `~/.m2/settings.xml`. ID должен совпадать с `publishingServerId` в pom.xml.
- **"GPG signing failed"** — проверить что ключ загружен в `keys.openpgp.org`, fingerprint подтверждён по email.
- **"Repository url not configured"** — забыт профиль `-P release` при `mvn deploy`.
- **Namespace `io.github.millersa` not verified** — зайти в https://central.sonatype.com → Namespaces → проверить статус.
