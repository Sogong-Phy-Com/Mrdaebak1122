import express from 'express';
import { getDatabase } from '../database';

const router = express.Router();

// Get all dinner types
router.get('/dinners', async (req, res) => {
  try {
    const db = getDatabase();
    const dinners = await db.all(`
      SELECT 
        dt.id,
        dt.name,
        dt.name_en,
        dt.base_price,
        dt.description
      FROM dinner_types dt
      ORDER BY dt.id
    `);

    // Get menu items for each dinner
    for (const dinner of dinners) {
      const menuItems = await db.all(`
        SELECT 
          mi.id,
          mi.name,
          mi.name_en,
          mi.price,
          mi.category,
          dmi.quantity
        FROM dinner_menu_items dmi
        JOIN menu_items mi ON dmi.menu_item_id = mi.id
        WHERE dmi.dinner_type_id = ?
      `, [dinner.id]);
      (dinner as any).menu_items = menuItems;
    }

    res.json(dinners);
  } catch (error: any) {
    console.error('Error fetching dinners:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Get all menu items
router.get('/items', async (req, res) => {
  try {
    const db = getDatabase();
    const items = await db.all('SELECT * FROM menu_items ORDER BY category, name');
    res.json(items);
  } catch (error: any) {
    console.error('Error fetching menu items:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Get serving style prices
router.get('/serving-styles', async (req, res) => {
  try {
    const styles = [
      { name: 'simple', name_ko: '심플', price_multiplier: 1.0, description: '플라스틱 접시와 플라스틱 컵, 종이 냅킨, 플라스틱 쟁반' },
      { name: 'grand', name_ko: '그랜드', price_multiplier: 1.3, description: '도자기 접시와 도자기 컵, 흰색 면 냅킨, 나무 쟁반' },
      { name: 'deluxe', name_ko: '디럭스', price_multiplier: 1.6, description: '꽃들이 있는 작은 꽃병, 도자기 접시와 도자기 컵, 린넨 냅킨, 나무 쟁반' }
    ];
    res.json(styles);
  } catch (error: any) {
    console.error('Error fetching serving styles:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

export default router;

