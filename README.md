# "Поисковый движок"
## Описание
Реализована backend - составляющая часть локального поискового движка.

Поисковый движок представляет из себя Spring-приложение, работающее с локально установленной базой данных MySQL, имеющее простой веб-интерфейс и API, через который им можно управлять и получать результаты поисковой выдачи по запросу.

Адреса сайтов, по которым движок осуществляет поиск, задаются перед запуском приложения в конфигурационном файле. 

Поисковый движок самостоятельно обходит все страницы заданных сайтов и индекесирует их, а так же может добавлять и переиндексировать отдельно заданные страниц этих сайтов.

Поиск фразы возможен на отдельно выбранном сайте или на всех проиндексированных.

Результаты поиска отдаются пользователю в ранжированном и сортированном виде.
## Стэк используемых технологий
Java

MySQL

Spring Boot
## Инструкция по локальному запуску проекта
Для успешного скачивания и подключения к проекту зависимостей из GitHub необходимо настроить Maven конфигурацию в файле settings.xml.
Для доступа требуется авторизации по токену.

Необходимо установить MySQL-сервер и создать в нем пустую базу данных search_engine. Параметры подключения указать в application.yaml.

Необходимо иметь установленный JDK.

