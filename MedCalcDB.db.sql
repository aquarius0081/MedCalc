BEGIN TRANSACTION;
CREATE TABLE "Computations" (
	`Id`	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
	`Name`	TEXT NOT NULL,
	`TypeId`	INTEGER NOT NULL,
	FOREIGN KEY(`TypeId`) REFERENCES ComputationTypes(Id)
);
INSERT INTO `Computations` (Id,Name,TypeId) VALUES (1,'СКФ name',1);
INSERT INTO `Computations` (Id,Name,TypeId) VALUES (2,'Test',2);
CREATE TABLE `ComputationTypes` (
	`Id`	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
	`Name`	TEXT NOT NULL
);
INSERT INTO `ComputationTypes` (Id,Name) VALUES (1,'СКФ');
INSERT INTO `ComputationTypes` (Id,Name) VALUES (2,'TestType');
CREATE TABLE "CompParams" (
	`Id`	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
	`Name`	TEXT NOT NULL,
	`TypeId`	INTEGER NOT NULL,
	`ComputationId`	INTEGER NOT NULL,
	FOREIGN KEY(`TypeId`) REFERENCES CompParamTypes(Id),
	FOREIGN KEY(`ComputationId`) REFERENCES `Computations`(`Id`)
);
INSERT INTO `CompParams` (Id,Name,TypeId,ComputationId) VALUES (1,'Вес',1,1);
INSERT INTO `CompParams` (Id,Name,TypeId,ComputationId) VALUES (2,'Рост',1,2);
CREATE TABLE `CompParamTypes` (
	`Id`	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
	`Name`	TEXT NOT NULL UNIQUE
);
INSERT INTO `CompParamTypes` (Id,Name) VALUES (1,'Integer');
COMMIT;
