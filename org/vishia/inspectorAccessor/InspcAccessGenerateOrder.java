package org.vishia.inspectorAccessor;

/**Support generating order numbers.
 * A Order number contains an information about the millisecond where it is created in realtime.
 * If more as one telegrams are send in the same millisecond, the value is incremented.
 * Wrap arround: 32 bit = 4 billion milliseconds, about 49 days. It means, a hanging telegram
 * can't have the same order. The millisecond-binding of order supports debugging.
 * @author Hartmut Schorrig
 *
 */
public class InspcAccessGenerateOrder
{

	int nLastOrder;
	
	InspcAccessGenerateOrder()
	{
		nLastOrder = (int)((System.currentTimeMillis() & 0x0fffffff));
	}
	
	int getNewOrder()
	{
		int order = (int)((System.currentTimeMillis() & 0x0fffffff)); 
		if( (order - nLastOrder) <=0){
			order = nLastOrder +1;
		}
		nLastOrder = order;
		return order;
	}
	
	
}
