-- 向已有数据库追加 2 位医生（补足到 20 位），不清空现有数据。
-- 执行：mysql -u root -p --default-character-set=utf8mb4 hda_db < backend/src/main/resources/add_doctors.sql
USE hda_db;

INSERT INTO doctor (name, title, hospital, department, speciality, introduction, status, create_time, update_time, deleted)
VALUES
('钱美玲', '副主任医师', '市康复医院',     '康复医学科', '偏瘫康复、骨折术后康复、慢性疼痛', '擅长老年脑卒中与骨折术后的系统康复训练，从业16年。', 1, NOW(), NOW(), 0),
('谢文斌', '主任医师',   '市第二人民医院', '泌尿外科',   '前列腺增生、泌尿系结石、尿失禁', '擅长老年前列腺疾病微创手术，累计完成手术2000余例。', 1, NOW(), NOW(), 0);
