# mono
Репозиторий для большинства плагинов, устанавливаются через /plu i [Название] <br />
Конфиги для плагинов находятся в папке conf <br />

# Обычные плагины
autorespawn - Автоматический респавн после смерти <br />
boosters - Выводит активные бустеры <br />
chancecmd - Выполняет команды с некоторым шансом /chancecmd <br />
chat - Заменяет ванильный чат <br />
clearchat - Заменяет мат, и немного мешает флудить <br />
closegui - Закрывает инвентарь игроку /closegui <br />
commandcooldown - Задержки для команд <br />
commandsbytime - Выполнение команд раз в некоторое время <br />
disableanycraft - Запрещает крафтить <br />
disablechunkunload - Отключает разгрузку чанков <br />
disablecorus - Запрещает использовать корус <br />
disablefeed - Отключает голод <br />
disablejoin - Запрещает вход при недостатке статистики <br />
disableitemdrop - Запрещает выбрасывать вещи <br />
disablemobexp - Отключает опыт с мобов <br />
disablepearl - Запрещает использовать эндер жемчуг <br />
disableshalkerinec - Запрещает ложить шалкеры в эндер сундук <br />
guicommand - Кастомные меню <br />
hideall - Скрывает всех игроков <br />
itemcommand - Выполнение команд при клике предметом <br />
iteminfo - Выводит информацию о предмете /iteminfo <br />
lowtps - Выполняет команды при низком тпс <br />
mobpickupitems - Запрещает подбирать мобам предметы <br />
morestatschat - Добавляет в чат статистику <br />
morestatscmd - /mystats, показывает статистику игрока <br />
moretext - Команды для префиксов к сообщениям, /et [Игрок] [Сообщение] <br />
randombylist - Выполняет случайную команду из списка, /randombylist <br />
randommoney - Выдает случайный процент от текущего количества денег <br />
removeitem - Убирает указанное количество предметов из руки <br />
removequitmsg - Убирает сообщение о выходе <br />
scrmsg - Команда для отправки тайтлов и экшен баров <br />
setcustomname - Установка кастомного имени игроку <br />
setnbt - Добавление NBT предмету в руке /setnbt <br />
sound - Проигрывание звука игроку <br />
statsdeath - Добавляет в статистику смерти <br />
statskill - Добавляет в статистику убийства <br />
texttower - Межсерверный чат <br />
trash - /мусорка, удаление лишних предметов <br />
triggermsg - Выполнение команд при словах в чате <br />
withoutsnow - Отключает появление блоков снега при снегопаде <br />
worldgen - Генерирует мир <br />
<br />
# Библеотеки
configs - Конфиги для плагинов, используется буквально везде<br />
cuboid - Кубоид с двумя точками Cuboid<br />
donate - Интерфейсы для доната, требуют реализации<br />
gui - Создание гуи ConfigInventory <br />
money - Управление балансом Money <br />
booster - Бустеры, /booster <br />
playerinfo - Получение читаемого имени игрока, суффиксы, префиксы, ник. PlayerReadable <br />
provide - Смотри исходники text <br />
text - Всякое связанное с отправкой текста <br />
cmdexec - ```Commands(listOf("text %player% lol")).exec(plugin, player)``` <br />
readablelong - ```100000000L.readable()``` -> 100,000,000 <br />
morestats - Статистика по каждому игроку /morestats, @StatsAPI <br />
spi - Хранение данных о игроке, смотри morestats <br />
tower - Используется для связывания нескольких серверов <br />
