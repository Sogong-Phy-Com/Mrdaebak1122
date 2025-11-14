import express from 'express';
import { body, validationResult } from 'express-validator';
import { getDatabase } from '../database';
import { authenticateToken, AuthRequest } from '../middleware/auth';

const router = express.Router();

// Process payment for order
router.post('/process', authenticateToken, [
  body('order_id').isInt(),
  body('payment_method').notEmpty(),
  body('card_number').optional(),
  body('card_expiry').optional(),
  body('card_cvv').optional()
], async (req: AuthRequest, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const { order_id, payment_method } = req.body;
    const db = getDatabase();
    const userId = req.userId!;

    // Verify order belongs to user
    const order = await db.get(
      'SELECT * FROM orders WHERE id = ? AND user_id = ?',
      [order_id, userId]
    );

    if (!order) {
      return res.status(404).json({ error: 'Order not found' });
    }

    if ((order as any).payment_status === 'paid') {
      return res.status(400).json({ error: 'Order already paid' });
    }

    // In a real application, you would integrate with a payment gateway here
    // For now, we'll simulate a successful payment
    // In production, use Stripe, PayPal, or another payment processor

    // Update order payment status
    await db.run(
      'UPDATE orders SET payment_status = ?, payment_method = ? WHERE id = ?',
      ['paid', payment_method, order_id]
    );

    res.json({
      message: 'Payment processed successfully',
      order_id: order_id,
      payment_status: 'paid'
    });
  } catch (error: any) {
    console.error('Payment processing error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

export default router;


