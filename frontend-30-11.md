# Status
Рустам:
    Импорт xodr в наш layout

Толя:
    Попробовал на готовых файла потестить. Вроде что-то возвращается. Как вытащить геометрию из xodr?
    Попробовал на своем xodr-е. nullPointerException в коде бека в районе junction. Геометрия не писалась пока

Егор П.:
    Редактирование графа?

Егор К.:
    Генерация модели дорожной сети по нашему layout.
    Нужно будет отдельно поддержать кривые дороги, когда они появятся в layout-е
    Возможно нужно будет начать считать модель перекрестка по внутренним дорогам

# Todo
    Пытаемся закончить визуализацию к последней неделе декабря (Вроде тогда у нас будет защита)
    Это 3 недели.
    1. Текстуры на дорогах ??

# Plan

Рустам:
    Залить внутренние дороги и геометрию дорог. Десериализация xodr.

Толя:
   Попробовать достать из очень простого xodr геометрию дорог и использовать ее.
   ПР на новый апи проверить

Егор П.:
    -//-

Егор К.:
    Адаптировать генерацию модели для кривых дорог и внутренних дорог перекрестка.
    Добавить поддержку для разного количества полос.
    Добавить пол на сцену, может скайбокс какой-нибудь, если это просто делается.
