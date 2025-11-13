import express from 'express';
import { getDatabase } from '../database';
import { authenticateToken, AuthRequest } from '../middleware/auth';

const router = express.Router();

// Get customer profile
router.get('/profile', authenticateToken, async (req: AuthRequest, res) => {
  try {
    const db = getDatabase();
    const userId = req.userId;

    const user = await db.get(
      'SELECT id, email, name, address, phone, created_at FROM users WHERE id = ?',
      [userId]
    );

    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }

    // Get order count for loyalty status
    const orderCount = await db.get<{ count: number }>(
      'SELECT COUNT(*) as count FROM orders WHERE user_id = ? AND payment_status = ?',
      [userId, 'paid']
    );

    res.json({
      ...user,
      order_count: orderCount?.count || 0,
      is_loyalty_customer: (orderCount?.count || 0) > 0
    });
  } catch (error: any) {
    console.error('Error fetching profile:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

export default router;

