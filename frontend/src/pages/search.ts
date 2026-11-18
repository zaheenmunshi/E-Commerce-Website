import { renderFooter } from '../components/footer.js';
import { renderNav } from '../components/nav.js';
import { renderProductGrid } from '../components/product-cards.js';
import { searchProducts } from '../lib/api.js';
import { qs } from '../lib/dom.js';

renderNav();
renderFooter();

const query = (new URLSearchParams(location.search).get('q') ?? '').trim();
qs('#search-key').textContent = query;
document.title = `Search Results for ${query}`;

const results = qs('.card-container');
if (query) {
  searchProducts(query)
    .then((products) => renderProductGrid(results, products))
    .catch(() => renderProductGrid(results, []));
} else {
  renderProductGrid(results, []);
}
