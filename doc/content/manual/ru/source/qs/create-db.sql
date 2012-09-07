create table SHOP_BUYER (
    ID uuid not null,             --Первичный ключ
    CREATE_TS timestamp,          --Когда создано (системное поле)
    CREATED_BY varchar(50),       --Кем  создано (системное поле)
    VERSION integer,              --Версия (системное поле)
    UPDATE_TS timestamp,          --Когда было последнее изменение (системное поле)
    UPDATED_BY varchar(50),       --Кто последний раз изменил сущность(системное поле)
    DELETE_TS timestamp,          --Когда удалено (системное поле)
    DELETED_BY varchar(50),       --Кем удалено (системное поле)

    FIRST_NAME varchar(100),      --Имя
    SURNAME varchar(100),         --Фамилия
    BIRTHDAY timestamp,           --Дата рождения
    EMAIL varchar(100),           --Электронная почта
    PHONE varchar(100),           --Номер мобильного телефона
    DELIVERY_ADDRESS varchar(255),--Адрес доставки

    primary key (ID)
)^
create table SHOP_DISCOUNT (
    ID uuid not null,			          --Первичный ключ
    CREATE_TS timestamp,          --Когда создано (системное поле)
    CREATED_BY varchar(50),       --Кем  создано (системное поле)
    VERSION integer,              --Версия (системное поле)
    UPDATE_TS timestamp,          --Когда было последнее изменение (системное поле)
    UPDATED_BY varchar(50),       --Кто последний раз изменил сущность(системное поле)
    DELETE_TS timestamp,          --Когда удалено (системное поле)
    DELETED_BY varchar(50),       --Кем удалено (системное поле)
    FROM_DATE timestamp,          --Дата начала скидки
 
    TILL_DATE timestamp,          --Дата окончания скидки
    BUYER_ID uuid,                --Идентификатор покупателя, внешний ключ
    MIN_QUANTITY integer,         --Минимальное количество товара
    PRICE numeric(19,2),          --Цена за единицу товара

    primary key (ID),
    constraint REF_DISCOUNT_BUYER foreign key (BUYER_ID) references SHOP_BUYER(ID)
)^