import express from 'express';
import { body, validationResult } from 'express-validator';
import { getDatabase } from '../database';
import { authenticateToken, AuthRequest } from '../middleware/auth';

const router = express.Router();

// Get all orders for authenticated user
router.get('/', authenticateToken, async (req: AuthRequest, res) => {
  try {
    const db = getDatabase();
    const userId = req.userId;

    const orders = await db.all(`
      SELECT 
        o.id,
        o.dinner_type_id,
        dt.name as dinner_name,
        dt.name_en as dinner_name_en,
        o.serving_style,
        o.delivery_time,
        o.delivery_address,
        o.total_price,
        o.status,
        o.payment_status,
        o.created_at
      FROM orders o
      JOIN dinner_types dt ON o.dinner_type_id = dt.id
      WHERE o.user_id = ?
      ORDER BY o.created_at DESC
    `, [userId]);

    // Get order items for each order
    for (const order of orders) {
      const items = await db.all(`
        SELECT 
          oi.id,
          oi.menu_item_id,
          mi.name,
          mi.name_en,
          mi.price,
          oi.quantity
        FROM order_items oi
        JOIN menu_items mi ON oi.menu_item_id = mi.id
        WHERE oi.order_id = ?
      `, [order.id]);
      (order as any).items = items;
    }

    res.json(orders);
  } catch (error: any) {
    console.error('Error fetching orders:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Get single order
router.get('/:id', authenticateToken, async (req: AuthRequest, res) => {
  try {
    const db = getDatabase();
    const userId = req.userId;
    const orderId = parseInt(req.params.id);

    const order = await db.get(`
      SELECT 
        o.*,
        dt.name as dinner_name,
        dt.name_en as dinner_name_en
      FROM orders o
      JOIN dinner_types dt ON o.dinner_type_id = dt.id
      WHERE o.id = ? AND o.user_id = ?
    `, [orderId, userId]);

    if (!order) {
      return res.status(404).json({ error: 'Order not found' });
    }

    const items = await db.all(`
      SELECT 
        oi.*,
        mi.name,
        mi.name_en,
        mi.price
      FROM order_items oi
      JOIN menu_items mi ON oi.menu_item_id = mi.id
      WHERE oi.order_id = ?
    `, [orderId]);

    (order as any).items = items;
    res.json(order);
  } catch (error: any) {
    console.error('Error fetching order:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Create new order
router.post('/', authenticateToken, [
  body('dinner_type_id').isInt(),
  body('serving_style').isIn(['simple', 'grand', 'deluxe']),
  body('delivery_time').notEmpty(),
  body('delivery_address').notEmpty(),
  body('items').isArray(),
  body('payment_method').optional()
], async (req: AuthRequest, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const { dinner_type_id, serving_style, delivery_time, delivery_address, items, payment_method } = req.body;
    const db = getDatabase();
    const userId = req.userId!;

    // Validate dinner type exists
    const dinner = await db.get('SELECT * FROM dinner_types WHERE id = ?', [dinner_type_id]);
    if (!dinner) {
      return res.status(400).json({ error: 'Invalid dinner type' });
    }

    // Validate serving style for Champagne Feast (only grand or deluxe)
    if (dinner_type_id === 4 && !['grand', 'deluxe'].includes(serving_style)) {
      return res.status(400).json({ error: 'Champagne Feast dinner can only be ordered with grand or deluxe style' });
    }

    // Calculate price
    const styleMultipliers: { [key: string]: number } = {
      simple: 1.0,
      grand: 1.3,
      deluxe: 1.6
    };
    let totalPrice = (dinner as any).base_price * styleMultipliers[serving_style];

    // Add item prices
    for (const item of items) {
      const menuItem = await db.get('SELECT price FROM menu_items WHERE id = ?', [item.menu_item_id]);
      if (menuItem) {
        totalPrice += (menuItem as any).price * item.quantity;
      }
    }

    // Apply loyalty discount (10% for returning customers)
    const orderCount = await db.get<{ count: number }>(
      'SELECT COUNT(*) as count FROM orders WHERE user_id = ? AND payment_status = ?',
      [userId, 'paid']
    );
    if (orderCount && orderCount.count > 0) {
      totalPrice = totalPrice * 0.9; // 10% discount
    }

    totalPrice = Math.round(totalPrice);

    // Create order
    const orderResult = await db.run(
      `INSERT INTO orders 
        (user_id, dinner_type_id, serving_style, delivery_time, delivery_address, total_price, payment_status, payment_method)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      [userId, dinner_type_id, serving_style, delivery_time, delivery_address, totalPrice, 'pending', payment_method || null]
    );

    // Add order items
    for (const item of items) {
      await db.run(
        'INSERT INTO order_items (order_id, menu_item_id, quantity) VALUES (?, ?, ?)',
        [orderResult.lastID, item.menu_item_id, item.quantity]
      );
    }

    res.status(201).json({
      message: 'Order created successfully',
      order_id: orderResult.lastID,
      total_price: totalPrice
    });
  } catch (error: any) {
    console.error('Error creating order:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Update order (modify items)
router.put('/:id', authenticateToken, [
  body('items').isArray().optional(),
  body('delivery_time').optional(),
  body('delivery_address').optional()
], async (req: AuthRequest, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const db = getDatabase();
    const userId = req.userId!;
    const orderId = parseInt(req.params.id);

    // Check if order exists and belongs to user
    const order = await db.get('SELECT * FROM orders WHERE id = ? AND user_id = ?', [orderId, userId]);
    if (!order) {
      return res.status(404).json({ error: 'Order not found' });
    }

    if ((order as any).status !== 'pending') {
      return res.status(400).json({ error: 'Can only modify pending orders' });
    }

    // Update delivery info if provided
    if (req.body.delivery_time) {
      await db.run('UPDATE orders SET delivery_time = ? WHERE id = ?', [req.body.delivery_time, orderId]);
    }
    if (req.body.delivery_address) {
      await db.run('UPDATE orders SET delivery_address = ? WHERE id = ?', [req.body.delivery_address, orderId]);
    }

    // Update items if provided
    if (req.body.items) {
      // Delete existing items
      await db.run('DELETE FROM order_items WHERE order_id = ?', [orderId]);

      // Add new items
      for (const item of req.body.items) {
        await db.run(
          'INSERT INTO order_items (order_id, menu_item_id, quantity) VALUES (?, ?, ?)',
          [orderId, item.menu_item_id, item.quantity]
        );
      }

      // Recalculate price
      const dinner = await db.get('SELECT base_price FROM dinner_types WHERE id = ?', [(order as any).dinner_type_id]);
      const styleMultipliers: { [key: string]: number } = {
        simple: 1.0,
        grand: 1.3,
        deluxe: 1.6
      };
      let totalPrice = (dinner as any).base_price * styleMultipliers[(order as any).serving_style];

      for (const item of req.body.items) {
        const menuItem = await db.get('SELECT price FROM menu_items WHERE id = ?', [item.menu_item_id]);
        if (menuItem) {
          totalPrice += (menuItem as any).price * item.quantity;
        }
      }

      const orderCount = await db.get<{ count: number }>(
        'SELECT COUNT(*) as count FROM orders WHERE user_id = ? AND payment_status = ?',
        [userId, 'paid']
      );
      if (orderCount && orderCount.count > 0) {
        totalPrice = totalPrice * 0.9;
      }

      totalPrice = Math.round(totalPrice);
      await db.run('UPDATE orders SET total_price = ? WHERE id = ?', [totalPrice, orderId]);
    }

    res.json({ message: 'Order updated successfully' });
  } catch (error: any) {
    console.error('Error updating order:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Cancel order
router.delete('/:id', authenticateToken, async (req: AuthRequest, res) => {
  try {
    const db = getDatabase();
    const userId = req.userId!;
    const orderId = parseInt(req.params.id);

    const order = await db.get('SELECT * FROM orders WHERE id = ? AND user_id = ?', [orderId, userId]);
    if (!order) {
      return res.status(404).json({ error: 'Order not found' });
    }

    if ((order as any).status === 'delivered' || (order as any).status === 'cooking') {
      return res.status(400).json({ error: 'Cannot cancel order that is already being prepared or delivered' });
    }

    await db.run('UPDATE orders SET status = ? WHERE id = ?', ['cancelled', orderId]);
    res.json({ message: 'Order cancelled successfully' });
  } catch (error: any) {
    console.error('Error cancelling order:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

export default router;


