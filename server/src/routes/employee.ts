import express from 'express';
import { getDatabase } from '../database';
import { authenticateToken, AuthRequest, requireRole } from '../middleware/auth';

const router = express.Router();

// Get all orders for employees
router.get('/orders', authenticateToken, requireRole('employee'), async (req: AuthRequest, res) => {
  try {
    const db = getDatabase();
    const { status } = req.query;

    let query = `
      SELECT 
        o.id,
        o.user_id,
        u.name as customer_name,
        u.phone as customer_phone,
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
      JOIN users u ON o.user_id = u.id
      JOIN dinner_types dt ON o.dinner_type_id = dt.id
    `;

    const params: any[] = [];
    if (status) {
      query += ' WHERE o.status = ?';
      params.push(status);
    }

    query += ' ORDER BY o.created_at DESC';

    const orders = await db.all(query, params);

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

// Update order status
router.patch('/orders/:id/status', authenticateToken, requireRole('employee'), async (req: AuthRequest, res) => {
  try {
    const { status } = req.body;
    const orderId = parseInt(req.params.id);

    if (!['pending', 'cooking', 'ready', 'out_for_delivery', 'delivered', 'cancelled'].includes(status)) {
      return res.status(400).json({ error: 'Invalid status' });
    }

    const db = getDatabase();
    await db.run('UPDATE orders SET status = ? WHERE id = ?', [status, orderId]);

    res.json({ message: 'Order status updated successfully' });
  } catch (error: any) {
    console.error('Error updating order status:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Assign order to employee
router.post('/orders/:id/assign', authenticateToken, requireRole('employee'), async (req: AuthRequest, res) => {
  try {
    const { assignment_type } = req.body; // 'cooking' or 'delivery'
    const orderId = parseInt(req.params.id);
    const employeeId = req.userId;

    if (!['cooking', 'delivery'].includes(assignment_type)) {
      return res.status(400).json({ error: 'Invalid assignment type' });
    }

    const db = getDatabase();

    // Check if employee exists
    const employee = await db.get('SELECT * FROM employees WHERE user_id = ?', [employeeId]);
    if (!employee) {
      return res.status(404).json({ error: 'Employee not found' });
    }

    // Create assignment
    await db.run(
      'INSERT INTO order_assignments (order_id, employee_id, assignment_type, status) VALUES (?, ?, ?, ?)',
      [orderId, (employee as any).id, assignment_type, 'assigned']
    );

    res.json({ message: 'Order assigned successfully' });
  } catch (error: any) {
    console.error('Error assigning order:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

export default router;


