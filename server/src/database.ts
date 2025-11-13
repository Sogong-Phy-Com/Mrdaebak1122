import sqlite3 from 'sqlite3';
import { promisify } from 'util';
import path from 'path';
import fs from 'fs';

const dataDir = path.join(__dirname, '../data');
const dbPath = path.join(dataDir, 'mrdabak.db');

// Ensure data directory exists
if (!fs.existsSync(dataDir)) {
  fs.mkdirSync(dataDir, { recursive: true });
}

export interface Database {
  run: (sql: string, params?: any[]) => Promise<{ lastID: number; changes: number }>;
  get: <T = any>(sql: string, params?: any[]) => Promise<T | undefined>;
  all: <T = any>(sql: string, params?: any[]) => Promise<T[]>;
  close: () => Promise<void>;
}

let db: Database | null = null;

export function getDatabase(): Database {
  if (!db) {
    throw new Error('Database not initialized');
  }
  return db;
}

export async function initDatabase(): Promise<void> {
  return new Promise((resolve, reject) => {
    const sqliteDb = new sqlite3.Database(dbPath, (err) => {
      if (err) {
        reject(err);
        return;
      }
      console.log('Connected to SQLite database');
    });

    // Promisify database methods
    db = {
      run: (sql: string, params?: any[]) => {
        return new Promise((resolve, reject) => {
          sqliteDb.run(sql, params, function(err) {
            if (err) reject(err);
            else resolve({ lastID: this.lastID, changes: this.changes });
          });
        });
      },
      get: <T = any>(sql: string, params?: any[]) => {
        return new Promise((resolve, reject) => {
          sqliteDb.get(sql, params, (err, row) => {
            if (err) reject(err);
            else resolve(row as T | undefined);
          });
        });
      },
      all: <T = any>(sql: string, params?: any[]) => {
        return new Promise((resolve, reject) => {
          sqliteDb.all(sql, params, (err, rows) => {
            if (err) reject(err);
            else resolve(rows as T[]);
          });
        });
      },
      close: () => {
        return new Promise((resolve, reject) => {
          sqliteDb.close((err) => {
            if (err) reject(err);
            else resolve();
          });
        });
      }
    };

    // Create tables
    createTables().then(() => {
      seedInitialData().then(() => resolve()).catch(reject);
    }).catch(reject);
  });
}

async function createTables(): Promise<void> {
  const database = getDatabase();

  // Users table
  await database.run(`
    CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      email TEXT UNIQUE NOT NULL,
      password TEXT NOT NULL,
      name TEXT NOT NULL,
      address TEXT NOT NULL,
      phone TEXT NOT NULL,
      role TEXT NOT NULL DEFAULT 'customer',
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `);

  // Dinner types table
  await database.run(`
    CREATE TABLE IF NOT EXISTS dinner_types (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      name_en TEXT NOT NULL,
      base_price INTEGER NOT NULL,
      description TEXT
    )
  `);

  // Menu items table
  await database.run(`
    CREATE TABLE IF NOT EXISTS menu_items (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      name_en TEXT NOT NULL,
      price INTEGER NOT NULL,
      category TEXT NOT NULL
    )
  `);

  // Dinner menu items (many-to-many)
  await database.run(`
    CREATE TABLE IF NOT EXISTS dinner_menu_items (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      dinner_type_id INTEGER NOT NULL,
      menu_item_id INTEGER NOT NULL,
      quantity INTEGER NOT NULL DEFAULT 1,
      FOREIGN KEY (dinner_type_id) REFERENCES dinner_types(id),
      FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
    )
  `);

  // Orders table
  await database.run(`
    CREATE TABLE IF NOT EXISTS orders (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id INTEGER NOT NULL,
      dinner_type_id INTEGER NOT NULL,
      serving_style TEXT NOT NULL,
      delivery_time TEXT NOT NULL,
      delivery_address TEXT NOT NULL,
      total_price INTEGER NOT NULL,
      status TEXT NOT NULL DEFAULT 'pending',
      payment_status TEXT NOT NULL DEFAULT 'pending',
      payment_method TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (user_id) REFERENCES users(id),
      FOREIGN KEY (dinner_type_id) REFERENCES dinner_types(id)
    )
  `);

  // Order items (customized items)
  await database.run(`
    CREATE TABLE IF NOT EXISTS order_items (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      order_id INTEGER NOT NULL,
      menu_item_id INTEGER NOT NULL,
      quantity INTEGER NOT NULL,
      FOREIGN KEY (order_id) REFERENCES orders(id),
      FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
    )
  `);

  // Employees table
  await database.run(`
    CREATE TABLE IF NOT EXISTS employees (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id INTEGER,
      employee_type TEXT NOT NULL,
      status TEXT NOT NULL DEFAULT 'available',
      FOREIGN KEY (user_id) REFERENCES users(id)
    )
  `);

  // Order assignments
  await database.run(`
    CREATE TABLE IF NOT EXISTS order_assignments (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      order_id INTEGER NOT NULL,
      employee_id INTEGER,
      assignment_type TEXT NOT NULL,
      status TEXT NOT NULL DEFAULT 'assigned',
      FOREIGN KEY (order_id) REFERENCES orders(id),
      FOREIGN KEY (employee_id) REFERENCES employees(id)
    )
  `);
}

async function seedInitialData(): Promise<void> {
  const database = getDatabase();

  // Check if data already exists
  const existingDinners = await database.all('SELECT COUNT(*) as count FROM dinner_types');
  if (existingDinners[0]?.count > 0) {
    return; // Data already seeded
  }

  // Insert menu items
  const menuItems = [
    { name: '와인', name_en: 'Wine', price: 15000, category: 'drink' },
    { name: '샴페인', name_en: 'Champagne', price: 50000, category: 'drink' },
    { name: '커피', name_en: 'Coffee', price: 5000, category: 'drink' },
    { name: '스테이크', name_en: 'Steak', price: 35000, category: 'food' },
    { name: '샐러드', name_en: 'Salad', price: 12000, category: 'food' },
    { name: '에그 스크램블', name_en: 'Scrambled Eggs', price: 8000, category: 'food' },
    { name: '베이컨', name_en: 'Bacon', price: 10000, category: 'food' },
    { name: '빵', name_en: 'Bread', price: 5000, category: 'food' },
    { name: '바게트빵', name_en: 'Baguette', price: 6000, category: 'food' },
    { name: '냅킨', name_en: 'Napkin', price: 0, category: 'accessory' },
    { name: '접시 장식', name_en: 'Plate Decoration', price: 0, category: 'accessory' }
  ];

  for (const item of menuItems) {
    await database.run(
      'INSERT INTO menu_items (name, name_en, price, category) VALUES (?, ?, ?, ?)',
      [item.name, item.name_en, item.price, item.category]
    );
  }

  // Get menu item IDs (assuming they're inserted in order)
  const wineId = 1;
  const champagneId = 2;
  const coffeeId = 3;
  const steakId = 4;
  const saladId = 5;
  const eggsId = 6;
  const baconId = 7;
  const breadId = 8;
  const baguetteId = 9;

  // Insert dinner types
  const valentineDinner = await database.run(
    'INSERT INTO dinner_types (name, name_en, base_price, description) VALUES (?, ?, ?, ?)',
    ['발렌타인 디너', 'Valentine Dinner', 60000, '와인과 스테이크가 하트 모양 접시와 큐피드 장식과 함께 제공']
  );

  await database.run(
    'INSERT INTO dinner_menu_items (dinner_type_id, menu_item_id, quantity) VALUES (?, ?, ?)',
    [valentineDinner.lastID, wineId, 1]
  );
  await database.run(
    'INSERT INTO dinner_menu_items (dinner_type_id, menu_item_id, quantity) VALUES (?, ?, ?)',
    [valentineDinner.lastID, steakId, 1]
  );

  const frenchDinner = await database.run(
    'INSERT INTO dinner_types (name, name_en, base_price, description) VALUES (?, ?, ?, ?)',
    ['프렌치 디너', 'French Dinner', 70000, '커피, 와인, 샐러드, 스테이크 제공']
  );

  await database.run('INSERT INTO dinner_menu_items (dinner_type_id, menu_item_id, quantity) VALUES (?, ?, ?)', [frenchDinner.lastID, coffeeId, 1]);
  await database.run('INSERT INTO dinner_menu_items (dinner_type_id, menu_item_id, quantity) VALUES (?, ?, ?)', [frenchDinner.lastID, wineId, 1]);
  await database.run('INSERT INTO dinner_menu_items (dinner_type_id, menu_item_id, quantity) VALUES (?, ?, ?)', [frenchDinner.lastID, saladId, 1]);
  await database.run('INSERT INTO dinner_menu_items (dinner_type_id, menu_item_id, quantity) VALUES (?, ?, ?)', [frenchDinner.lastID, steakId, 1]);

  const englishDinner = await database.run(
    'INSERT INTO dinner_types (name, name_en, base_price, description) VALUES (?, ?, ?, ?)',
    ['잉글리시 디너', 'English Dinner', 65000, '에그 스크램블, 베이컨, 빵, 스테이크 제공']
  );

  await database.run('INSERT INTO dinner_menu_items (dinner_type_id, menu_item_id, quantity) VALUES (?, ?, ?)', [englishDinner.lastID, eggsId, 1]);
  await database.run('INSERT INTO dinner_menu_items (dinner_type_id, menu_item_id, quantity) VALUES (?, ?, ?)', [englishDinner.lastID, baconId, 1]);
  await database.run('INSERT INTO dinner_menu_items (dinner_type_id, menu_item_id, quantity) VALUES (?, ?, ?)', [englishDinner.lastID, breadId, 1]);
  await database.run('INSERT INTO dinner_menu_items (dinner_type_id, menu_item_id, quantity) VALUES (?, ?, ?)', [englishDinner.lastID, steakId, 1]);

  const champagneDinner = await database.run(
    'INSERT INTO dinner_types (name, name_en, base_price, description) VALUES (?, ?, ?, ?)',
    ['샴페인 축제 디너', 'Champagne Feast Dinner', 120000, '2인 식사, 샴페인 1병, 바게트빵 4개, 커피 포트 1개, 와인, 스테이크']
  );

  await database.run('INSERT INTO dinner_menu_items (dinner_type_id, menu_item_id, quantity) VALUES (?, ?, ?)', [champagneDinner.lastID, champagneId, 1]);
  await database.run('INSERT INTO dinner_menu_items (dinner_type_id, menu_item_id, quantity) VALUES (?, ?, ?)', [champagneDinner.lastID, baguetteId, 4]);
  await database.run('INSERT INTO dinner_menu_items (dinner_type_id, menu_item_id, quantity) VALUES (?, ?, ?)', [champagneDinner.lastID, coffeeId, 1]);
  await database.run('INSERT INTO dinner_menu_items (dinner_type_id, menu_item_id, quantity) VALUES (?, ?, ?)', [champagneDinner.lastID, wineId, 1]);
  await database.run('INSERT INTO dinner_menu_items (dinner_type_id, menu_item_id, quantity) VALUES (?, ?, ?)', [champagneDinner.lastID, steakId, 1]);

  console.log('Initial data seeded successfully');
}

