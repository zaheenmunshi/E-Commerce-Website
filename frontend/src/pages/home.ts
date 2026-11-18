import { renderFooter } from '../components/footer.js';
import { renderNav } from '../components/nav.js';
import { renderProductSlider } from '../components/product-cards.js';
import { productsByTag } from '../lib/api.js';
import { qs } from '../lib/dom.js';

const SLIDERS = [
  { container: '#men-products', tag: 'men', title: 'men' },
  { container: '#shoes-products', tag: 'shoes', title: 'shoes' },
];

renderNav();
renderFooter();

for (const slider of SLIDERS) {
  productsByTag(slider.tag)
    .then((products) => renderProductSlider(qs(slider.container), slider.title, products))
    .catch((error: unknown) => console.error(`could not load ${slider.tag} products`, error));
}
