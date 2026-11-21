CREATE TABLE IF NOT EXISTS treks (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  region VARCHAR(100) NOT NULL,
  difficulty VARCHAR(40) NOT NULL,
  days INT NOT NULL,
  max_altitude INT NOT NULL,
  price INT NOT NULL COMMENT 'Sample per-person price in Nepalese rupees (NPR)',
  rating DOUBLE NOT NULL DEFAULT 0,
  reviews INT NOT NULL DEFAULT 0,
  description VARCHAR(700),
  image_url VARCHAR(700)
);
CREATE TABLE IF NOT EXISTS guide_knowledge (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  question VARCHAR(500) NOT NULL,
  answer VARCHAR(1500) NOT NULL
);
CREATE TABLE IF NOT EXISTS users (
  username VARCHAR(50) NOT NULL PRIMARY KEY,
  password VARCHAR(500) NOT NULL,
  enabled BOOLEAN NOT NULL
);
CREATE TABLE IF NOT EXISTS authorities (
  username VARCHAR(50) NOT NULL,
  authority VARCHAR(50) NOT NULL,
  CONSTRAINT fk_authorities_users FOREIGN KEY(username) REFERENCES users(username) ON DELETE CASCADE,
  UNIQUE KEY ix_auth_username(authority, username)
);
CREATE TABLE IF NOT EXISTS email_verifications (
  username VARCHAR(50) NOT NULL PRIMARY KEY,
  token_hash CHAR(64) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_email_verification_user FOREIGN KEY(username) REFERENCES users(username) ON DELETE CASCADE
);
ALTER TABLE email_verifications ADD COLUMN IF NOT EXISTS attempts INT NOT NULL DEFAULT 0;
CREATE TABLE IF NOT EXISTS equipment (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  category VARCHAR(100) NOT NULL,
  price INT NOT NULL,
  rental_price INT NOT NULL DEFAULT 0,
  stock INT NOT NULL DEFAULT 0,
  image_url VARCHAR(700),
  status VARCHAR(40) NOT NULL DEFAULT 'Available'
);
CREATE TABLE IF NOT EXISTS trek_guides (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  region VARCHAR(100) NOT NULL,
  experience VARCHAR(100),
  bio VARCHAR(1200),
  languages VARCHAR(500),
  specialties VARCHAR(700),
  certifications VARCHAR(700),
  phone VARCHAR(40),
  email VARCHAR(190),
  rating DOUBLE NOT NULL DEFAULT 0,
  image_url VARCHAR(700),
  status VARCHAR(40) NOT NULL DEFAULT 'Active'
);
CREATE TABLE IF NOT EXISTS guide_bookings (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  guide_id BIGINT NULL,
  guide_name VARCHAR(150) NOT NULL,
  customer_name VARCHAR(150) NOT NULL,
  customer_email VARCHAR(190) NOT NULL,
  customer_phone VARCHAR(40),
  trek_name VARCHAR(180) NOT NULL,
  start_date DATE NOT NULL,
  travelers INT NOT NULL DEFAULT 1,
  notes VARCHAR(1000),
  status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
  decision_email_status VARCHAR(30),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_guide_booking_guide FOREIGN KEY(guide_id) REFERENCES trek_guides(id) ON DELETE SET NULL
);
CREATE TABLE IF NOT EXISTS trek_routes (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  trek_id BIGINT NOT NULL,
  day_number INT NOT NULL,
  title VARCHAR(250) NOT NULL,
  description VARCHAR(1000),
  distance_km DOUBLE,
  altitude_m INT,
  CONSTRAINT fk_routes_trek FOREIGN KEY(trek_id) REFERENCES treks(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS bookings (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NULL,
  customer_name VARCHAR(150) NULL,
  customer_email VARCHAR(190) NULL,
  customer_phone VARCHAR(30) NULL,
  trek_id BIGINT NOT NULL,
  start_date DATE NOT NULL,
  travelers INT NOT NULL DEFAULT 1,
  amount_npr INT NOT NULL,
  payment_method VARCHAR(40) NULL,
  payment_status VARCHAR(40) NOT NULL DEFAULT 'Pending',
  status VARCHAR(40) NOT NULL DEFAULT 'Requested',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_bookings_user FOREIGN KEY(username) REFERENCES users(username),
  CONSTRAINT fk_bookings_trek FOREIGN KEY(trek_id) REFERENCES treks(id)
);
CREATE TABLE IF NOT EXISTS trek_plans (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  trek_id BIGINT NOT NULL,
  start_date DATE,
  travelers INT NOT NULL DEFAULT 1,
  notes VARCHAR(1000),
  status VARCHAR(40) NOT NULL DEFAULT 'Planning',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_plans_user FOREIGN KEY(username) REFERENCES users(username),
  CONSTRAINT fk_plans_trek FOREIGN KEY(trek_id) REFERENCES treks(id)
);
CREATE TABLE IF NOT EXISTS rentals (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NULL,
  customer_name VARCHAR(150) NULL,
  customer_email VARCHAR(190) NULL,
  customer_phone VARCHAR(30) NULL,
  equipment_id BIGINT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  amount_npr INT NOT NULL,
  payment_method VARCHAR(40) NULL,
  payment_status VARCHAR(40) NOT NULL DEFAULT 'Pending',
  esewa_transaction_uuid VARCHAR(80) NULL,
  payment_reference VARCHAR(100) NULL,
  decision_email_status VARCHAR(30) NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'Requested',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_rentals_user FOREIGN KEY(username) REFERENCES users(username),
  CONSTRAINT fk_rentals_equipment FOREIGN KEY(equipment_id) REFERENCES equipment(id)
);
CREATE TABLE IF NOT EXISTS orders (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  equipment_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  amount_npr INT NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'Requested',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_orders_user FOREIGN KEY(username) REFERENCES users(username),
  CONSTRAINT fk_orders_equipment FOREIGN KEY(equipment_id) REFERENCES equipment(id)
);
CREATE TABLE IF NOT EXISTS reviews (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  trek_id BIGINT NOT NULL,
  rating INT NOT NULL,
  review_text VARCHAR(1500) NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'Pending review',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_reviews_user FOREIGN KEY(username) REFERENCES users(username),
  CONSTRAINT fk_reviews_trek FOREIGN KEY(trek_id) REFERENCES treks(id)
);
CREATE TABLE IF NOT EXISTS announcements (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(250) NOT NULL,
  body VARCHAR(1500) NOT NULL,
  severity VARCHAR(40) NOT NULL DEFAULT 'Info',
  published BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
