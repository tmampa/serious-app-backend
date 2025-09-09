-- Update existing books to set available=true
UPDATE book SET available = true WHERE available IS NULL;

-- Update existing borrowing records to set fine_amount=0
UPDATE borrowing_record SET fine_amount = 0 WHERE fine_amount IS NULL;
